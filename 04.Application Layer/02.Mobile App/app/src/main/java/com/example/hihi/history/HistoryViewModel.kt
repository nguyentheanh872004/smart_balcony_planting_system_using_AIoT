package com.example.hihi.history

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hihi.data.HistoryData
import com.example.hihi.network.ApiClient
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {

    var history by mutableStateOf<List<HistoryData>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun loadHistory(limit: Int = 50) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val resp = ApiClient.api.getHistory(limit)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null && body.success) {
                        history = body.data ?: emptyList()
                    } else {
                        errorMessage = body?.error ?: "Empty response"
                    }
                } else {
                    errorMessage = "HTTP ${resp.code()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.message ?: "Failed to load history"
            } finally {
                isLoading = false
            }
        }
    }
}