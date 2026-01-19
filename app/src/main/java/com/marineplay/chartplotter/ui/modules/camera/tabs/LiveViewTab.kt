package com.marineplay.chartplotter.ui.modules.camera.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marineplay.chartplotter.ui.modules.camera.CameraTheme
import com.marineplay.chartplotter.ui.modules.camera.components.*
import com.marineplay.chartplotter.ui.modules.camera.models.CameraPosition
import com.marineplay.chartplotter.ui.modules.camera.models.ViewMode

@Composable
fun LiveViewTab(
    selectedCamera: CameraPosition,
    viewMode: ViewMode,
    isRecording: Boolean,
    onCameraSelected: (CameraPosition) -> Unit,
    onViewModeChanged: (ViewMode) -> Unit,
    onRecordingChanged: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 카메라 피드 영역
        CameraFeedGrid(
            selectedCamera = selectedCamera,
            viewMode = viewMode,
            modifier = Modifier.fillMaxSize()
        )

        // 상단 상태바
        TopStatusBar(
            isRecording = isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CameraTheme.primaryColor.copy(alpha = 0.95f),
                            CameraTheme.primaryColor.copy(alpha = 0.0f)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )

        // 하단 컨트롤 패널
        BottomControlPanel(
            selectedCamera = selectedCamera,
            viewMode = viewMode,
            onCameraSelected = onCameraSelected,
            onViewModeChanged = onViewModeChanged,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 24.dp)
        )
    }
}

