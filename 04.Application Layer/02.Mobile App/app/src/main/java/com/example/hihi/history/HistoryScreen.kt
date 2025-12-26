package com.example.hihi.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HistoryScreen() {

    val vm: HistoryViewModel = viewModel()

    LaunchedEffect(Unit) {
        vm.loadHistory()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = 72.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "📊 History Data",
            style = MaterialTheme.typography.titleLarge
        )

        if (vm.isLoading) {
            CircularProgressIndicator()
            return@Column
        }

        if (vm.errorMessage != null) {
            Text("❌ ${vm.errorMessage}")
            return@Column
        }

        if (vm.history.isEmpty()) {
            Text("No history data")
            return@Column
        }

        Text("Records: ${vm.history.size}")

        val tempList = vm.history.mapNotNull { it.temp }
        val humiList = vm.history.mapNotNull { it.humi }
        val soilList = vm.history.mapNotNull { it.soil }

        Text("🌡 Temperature")
        SimpleLineChart(
            values = tempList,
            label = "Temperature",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Text("💧 Humidity")
        SimpleLineChart(
            values = humiList,
            label = "Humidity",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Text("🌱 Soil Moisture")
        SimpleLineChart(
            values = soilList,
            label = "Soil",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
    }
}