package com.marineplay.chartplotter.ui.modules.camera.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.ui.modules.camera.CameraTheme
import com.marineplay.chartplotter.ui.modules.camera.models.CameraPosition
import com.marineplay.chartplotter.ui.modules.camera.models.ViewMode

@Composable
fun BottomControlPanel(
    selectedCamera: CameraPosition,
    viewMode: ViewMode,
    onCameraSelected: (CameraPosition) -> Unit,
    onViewModeChanged: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 카메라 선택 버튼 (단일 화면 모드일 때만)
        if (viewMode == ViewMode.SINGLE) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CameraPosition.values().forEach { position ->
                    Button(
                        onClick = { onCameraSelected(position) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCamera == position) {
                                CameraTheme.primaryColor
                            } else {
                                CameraTheme.surfaceColor
                            },
                            contentColor = if (selectedCamera == position) {
                                Color.White
                            } else {
                                Color(0xFF1A202C) // 검은색/진한 회색
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = position.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // 화면 분할 모드 버튼
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.align(Alignment.End)
        ) {
            ViewModeButton(
                viewMode = ViewMode.SINGLE,
                currentViewMode = viewMode,
                icon = Icons.Default.AspectRatio,
                label = "1분할",
                onClick = { onViewModeChanged(ViewMode.SINGLE) },
                isSelected = viewMode == ViewMode.SINGLE
            )
            ViewModeButton(
                viewMode = ViewMode.DUAL,
                currentViewMode = viewMode,
                icon = Icons.Default.ViewComfy,
                label = "2분할",
                onClick = { onViewModeChanged(ViewMode.DUAL) },
                isSelected = viewMode == ViewMode.DUAL
            )
            ViewModeButton(
                viewMode = ViewMode.QUAD,
                currentViewMode = viewMode,
                icon = Icons.Default.Apps,
                label = "4분할",
                onClick = { onViewModeChanged(ViewMode.QUAD) },
                isSelected = viewMode == ViewMode.QUAD
            )
        }
    }
}

@Composable
private fun ViewModeButton(
    viewMode: ViewMode,
    currentViewMode: ViewMode,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) CameraTheme.primaryColor else CameraTheme.surfaceColor,
            contentColor = if (isSelected) Color.White else Color(0xFF1A202C) // 검은색/진한 회색
        ),
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
    }
}

