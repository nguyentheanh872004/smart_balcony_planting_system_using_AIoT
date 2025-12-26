package com.example.hihi.dashboard

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hihi.data.SensorData
import com.example.hihi.network.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý:
 * - polling tự động (startAutoRefresh / stopAutoRefresh)
 * - load thủ công (loadLatestData)
 * - gửi control (pump/mist/mode) với optimistic update, pause polling trong khi gửi
 *
 * Yêu cầu: ApiClient.api.updateControl(...) trả Response<ApiResult<SensorData>>
 */
class DashboardViewModel : ViewModel() {

    var sensorData by mutableStateOf<SensorData?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Nếu true => UI nên disable các control (tránh gửi nhiều request chồng)
    var controlRequestInProgress by mutableStateOf(false)
        private set

    private var pollingJob: Job? = null
    private var pollingIntervalMs: Long = DEFAULT_POLLING_MS

    // track để restart polling nếu trước đó đang chạy
    private var pollingWasRunning = false

    companion object {
        // Mặc định poll mỗi 3 giây — bạn có thể truyền giá trị khác khi gọi startAutoRefresh()
        private const val DEFAULT_POLLING_MS = 3_000L
    }

    /**
     * One-shot manual load (vẫn giữ để gọi khi user nhấn refresh)
     */
    fun loadLatestData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val result = ApiClient.api.getLatestData()
                sensorData = result
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.message ?: "Failed to load current data"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Bắt đầu polling liên tục ở background; nếu đang có polling thì không làm gì.
     * Gọi được từ UI (ví dụ trong LaunchedEffect khi screen hiển thị).
     */
    fun startAutoRefresh(intervalMs: Long = DEFAULT_POLLING_MS) {
        pollingIntervalMs = intervalMs
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            // Lần đầu có thể tải ngay để UI không phải chờ interval
            try {
                val initial = ApiClient.api.getLatestData()
                sensorData = initial
                errorMessage = null
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.message ?: "Failed to refresh data"
            }

            while (isActive) {
                try {
                    delay(pollingIntervalMs)
                    val result = ApiClient.api.getLatestData()
                    sensorData = result
                    errorMessage = null
                } catch (e: Exception) {
                    // Không phá vỡ loop — chỉ báo lỗi và tiếp tục thử lại sau interval
                    e.printStackTrace()
                    errorMessage = e.message ?: "Failed to refresh data"
                }
            }
        }
    }

    /**
     * Dừng polling (gọi khi screen bị dispose hoặc app đi background tùy UI lifecycle control)
     */
    fun stopAutoRefresh() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        stopAutoRefresh()
        super.onCleared()
    }

    /**
     * Gửi control request (pump/mist/mode).
     * - Tạm dừng polling trước khi gửi để tránh race.
     * - Optimistic update: cập nhật UI sớm khi đã có sensorData (có thể rollback nếu lỗi).
     * - Sau response thành công: ưu tiên dùng data trả về từ server (body.data) nếu có,
     *   hoặc gọi getLatestData() để đồng bộ.
     */
    private fun sendControlRequest(
        pump: Int? = null,
        mist: Int? = null,
        mode: Int? = null,
        optimistic: Boolean = true
    ) {
        viewModelScope.launch {
            if (controlRequestInProgress) return@launch
            controlRequestInProgress = true
            errorMessage = null

            // Tạm dừng polling (nếu đang chạy) để tránh race với update
            pollingWasRunning = pollingJob?.isActive == true
            if (pollingWasRunning) stopAutoRefresh()

            // Lưu state cũ để rollback khi cần
            val oldState: SensorData? = sensorData?.copy()

            // Optimistic update (chỉ khi đã có dữ liệu hiện tại)
            if (optimistic && sensorData != null) {
                val next = sensorData!!.copy(
                    pump = pump ?: sensorData!!.pump,
                    mist = mist ?: sensorData!!.mist,
                    mode = mode ?: sensorData!!.mode
                )
                sensorData = next
            }

            try {
                val response = ApiClient.api.updateControl(pump = pump, mist = mist, mode = mode)
                if (!response.isSuccessful) {
                    throw Exception("Server returned ${response.code()}")
                }
                val body = response.body()
                if (body == null) {
                    // Nếu server trả body null, fallback gọi getLatestData()
                    val latest = ApiClient.api.getLatestData()
                    sensorData = latest
                } else {
                    if (!body.success) {
                        throw Exception(body.error ?: "Server reported failure")
                    }
                    // Nếu server trả data (mapped to SensorData), dùng luôn
                    body.data?.let {
                        sensorData = it
                    } ?: run {
                        // nếu không có data trong body, fetch lại latest
                        val latest = ApiClient.api.getLatestData()
                        sensorData = latest
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.message ?: "Failed to send control"
                // rollback nếu optimistic
                if (optimistic) {
                    sensorData = oldState
                }
            } finally {
                controlRequestInProgress = false
                // khởi động lại polling nếu trước đó đang chạy
                if (pollingWasRunning) startAutoRefresh(pollingIntervalMs)
            }
        }
    }

    // Các wrapper cho từng control
    fun updatePump(value: Int) {
        sendControlRequest(pump = value)
    }

    fun updateMist(value: Int) {
        sendControlRequest(mist = value)
    }

    fun updateMode(value: Int) {
        sendControlRequest(mode = value)
    }
}