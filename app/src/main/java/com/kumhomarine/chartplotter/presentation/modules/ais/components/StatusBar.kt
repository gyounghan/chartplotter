package com.kumhomarine.chartplotter.presentation.modules.ais.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumhomarine.chartplotter.presentation.modules.ais.AISTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 상단 상태바 (AIS design 기준)
 * GPS 신호, AIS 수신, NMEA2000 상태 및 현재 시간 표시
 */
@Composable
fun AISStatusBar(
    gpsFix: Boolean = true,
    aisRx: Boolean = true,
    nmea: Boolean = true,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }
    val timeStr = remember(currentTime) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(currentTime))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.Black)
            .drawBehind {
                drawLine(
                    color = AISTheme.borderColor,
                    strokeWidth = 2.dp.toPx(),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height)
                )
            }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            StatusIndicator(enabled = gpsFix, label = "GPS 신호")
            StatusIndicator(enabled = aisRx, label = "AIS 수신")
            StatusIndicator(enabled = nmea, label = "NMEA2000")
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = timeStr,
            fontSize = 14.sp,
            color = AISTheme.textPrimary
        )
    }
}

@Composable
private fun StatusIndicator(
    enabled: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(if (enabled) AISTheme.safe else AISTheme.danger)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = AISTheme.textPrimary
        )
    }
}
