package com.example.hihi.network

import com.example.hihi.data.SensorData
import com.example.hihi.data.HistoryData
import com.example.hihi.data.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("api/get_latest.php")
    suspend fun getLatestData(): SensorData

    // Trả wrapper { success, data: [...] }
    @GET("api/get_history.php")
    suspend fun getHistory(
        @Query("limit") limit: Int = 50
    ): Response<ApiResult<List<HistoryData>>>

    @FormUrlEncoded
    @POST("api/control.php")
    suspend fun updateControl(
        @Field("pump") pump: Int? = null,
        @Field("mist") mist: Int? = null,
        @Field("mode") mode: Int? = null
    ): Response<ApiResult<SensorData>>

    @POST("api/login.php")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

}