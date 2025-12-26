package com.example.hihi.history

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@Composable
fun SimpleLineChart(
    values: List<Float>,
    label: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LineChart(context).apply {

                if (values.isEmpty()) {
                    clear()
                    return@apply
                }

                val entries = values.mapIndexed { index, v ->
                    Entry(index.toFloat(), v)
                }

                val dataSet = LineDataSet(entries, label).apply {
                    color = Color.BLUE
                    setDrawCircles(false)
                    lineWidth = 2f
                    setDrawValues(false)
                }

                data = LineData(dataSet)

                description.isEnabled = false
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                axisLeft.setDrawGridLines(true)

                invalidate()
            }
        }
    )
}
