package com.example.hihi.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SensorCard(
    title: String,
    value: String,
    unit: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(color.copy(alpha = 0.12f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "$value $unit",
                    style = MaterialTheme.typography.titleLarge,
                    color = color
                )
            }
        }
    }
}

/* ===== HELPER MÀU THEO NGƯỠNG ===== */
fun tempColor(t: Float?): Color = when {
    t == null -> Color.Gray
    t < 20 -> Color(0xFF2196F3)
    t < 30 -> Color(0xFF4CAF50)
    else -> Color(0xFFF44336)
}

fun humiColor(h: Float?): Color = when {
    h == null -> Color.Gray
    h < 40 -> Color(0xFFF44336)
    h < 70 -> Color(0xFF4CAF50)
    else -> Color(0xFF2196F3)
}

fun soilColor(s: Float?): Color = when {
    s == null -> Color.Gray
    s < 30 -> Color(0xFFF44336)
    else -> Color(0xFF4CAF50)
}
