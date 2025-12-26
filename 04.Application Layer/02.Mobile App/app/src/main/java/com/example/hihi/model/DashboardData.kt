package com.example.hihi.model

data class DashboardData(
    val temperature: Float,
    val humidity: Int,
    val light: Int,
    val pm25: Int,
    val soilMoisture: Int,
    val pumpOn: Boolean,
    val mistOn: Boolean,
    val aiMode: Boolean
)