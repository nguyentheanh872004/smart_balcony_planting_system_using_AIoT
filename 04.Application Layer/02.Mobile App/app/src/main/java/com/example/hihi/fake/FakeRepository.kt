package com.example.hihi.fake

import com.example.hihi.model.DashboardData

object FakeRepository {

    fun getDashboardData(): DashboardData {
        return DashboardData(
            temperature = 25.5f,
            humidity = 60,
            light = 300,
            pm25 = 15,
            soilMoisture = 40,
            pumpOn = false,
            mistOn = false,
            aiMode = true
        )
    }
}
