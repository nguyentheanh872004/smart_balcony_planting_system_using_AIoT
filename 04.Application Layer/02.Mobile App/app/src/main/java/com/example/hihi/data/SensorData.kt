package com.example.hihi.data

data class SensorData(
    val id: Int? = null,
    val time: String? = null,
    val temp: Float? = null,
    val humi: Float? = null,
    val pressure: Float? = null,
    val light: Int? = null,
    val rain: Float? = null,
    val pm25: Int? = null,
    val soil: Float? = null,
    val pump: Int = 0,
    val mist: Int = 0,
    val mode: Int = 0
)