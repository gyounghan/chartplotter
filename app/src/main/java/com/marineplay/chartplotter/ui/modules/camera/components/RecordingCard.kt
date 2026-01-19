package com.marineplay.chartplotter.ui.modules.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.ui.modules.camera.CameraTheme
import com.marineplay.chartplotter.ui.modules.camera.models.EventType
import com.marineplay.chartplotter.ui.modules.camera.models.Recording

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingCard(
    recording: Recording,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CameraTheme.surfaceColor),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CameraTheme.borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 썸네일
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(80.dp)
                    .background(CameraTheme.backgroundColor, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = CameraTheme.textSecondary,
                    modifier = Modifier.size(32.dp)
                )
                if (recording.locked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(CameraTheme.primaryColor, RoundedCornerShape(4.dp))
                            .padding(4.dp)
                            .padding(top = 4.dp, end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // 정보
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recording.time,
                        color = CameraTheme.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .background(recording.eventType.color, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = recording.eventType.label,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                    if (recording.locked) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CameraTheme.primaryColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "잠김",
                                color = CameraTheme.primaryColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "카메라: ${recording.cameraLabel}",
                        color = CameraTheme.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "길이: ${formatDuration(recording.duration)}",
                        color = CameraTheme.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}

