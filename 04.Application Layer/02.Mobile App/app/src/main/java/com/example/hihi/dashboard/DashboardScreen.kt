package com.example.hihi.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DashboardScreen() {

    val vm: DashboardViewModel = viewModel()

    // Bắt đầu polling khi composable mount và dừng khi unmount
    DisposableEffect(Unit) {
        vm.startAutoRefresh() // mặc định interval 3000ms, truyền tham số nếu muốn khác
        onDispose {
            vm.stopAutoRefresh()
        }
    }

    val d = vm.sensorData

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp, start = 16.dp, end = 16.dp)
    ) {

        Text("📡 Current Sensor Data", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (vm.isLoading) {
            CircularProgressIndicator()
            return@Column
        }

        if (vm.errorMessage != null) {
            Text("❌ ${vm.errorMessage}")
            return@Column
        }

        if (d == null) {
            Text("No data")
            return@Column
        }

        /* ===== SENSOR GRID ===== */
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(420.dp)
        ) {

            item {
                SensorCard("Temperature", "${d.temp}", "°C", tempColor(d.temp))
            }
            item {
                SensorCard("Humidity", "${d.humi}", "%", humiColor(d.humi))
            }
            item {
                SensorCard("Pressure", "${d.pressure}", "hPa", MaterialTheme.colorScheme.primary)
            }
            item {
                SensorCard("Light", "${d.light}", "", MaterialTheme.colorScheme.primary)
            }
            item {
                SensorCard(
                    "Rain",
                    d.rain?.let { "%.2f".format(it) } ?: "N/A",
                    "%",
                    MaterialTheme.colorScheme.secondary
                )
            }
            item {
                SensorCard("PM2.5", "${d.pm25}", "µg/m³", MaterialTheme.colorScheme.error)
            }
            item {
                SensorCard("Soil", "${d.soil}", "%", soilColor(d.soil))
            }
            item {
                SensorCard("Pump", if (d.pump == 1) "ON" else "OFF", "", MaterialTheme.colorScheme.tertiary)
            }
            item {
                SensorCard("Mist", if (d.mist == 1) "ON" else "OFF", "", MaterialTheme.colorScheme.tertiary)
            }
        }

        Spacer(Modifier.height(16.dp))

        /* ===== AI MODE ===== */
        Text("🤖 AI Control", style = MaterialTheme.typography.titleMedium)

        // Local UI state để phản hồi ngay khi user bấm
        var aiMode by remember { mutableStateOf(d.mode == 1) }
        var pump by remember { mutableStateOf(d.pump == 1) }
        var mist by remember { mutableStateOf(d.mist == 1) }

        // Đồng bộ lại local state mỗi khi sensorData (d) thay đổi từ server
        LaunchedEffect(d) {
            if (d != null) {
                aiMode = d.mode == 1
                pump = d.pump == 1
                mist = d.mist == 1
            }
        }

        ManualSwitch(
            title = "AI Mode",
            checked = aiMode
        ) {
            aiMode = it
            vm.updateMode(if (it) 1 else 0)
        }

        /* ===== MANUAL MODE ===== */
        if (!aiMode) {
            Divider()

            ManualSwitch(
                title = "Water Pump",
                checked = pump
            ) {
                pump = it
                vm.updatePump(if (it) 1 else 0)
            }

            ManualSwitch(
                title = "Mist Spray",
                checked = mist
            ) {
                mist = it
                vm.updateMist(if (it) 1 else 0)
            }
        }
    }
}