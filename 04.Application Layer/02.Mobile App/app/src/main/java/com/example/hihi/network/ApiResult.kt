package com.example.hihi.network

data class ApiResult<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: String? = null
)