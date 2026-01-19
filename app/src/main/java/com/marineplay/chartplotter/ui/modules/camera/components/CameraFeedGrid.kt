package com.marineplay.chartplotter.ui.modules.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.marineplay.chartplotter.ui.modules.camera.CameraTheme
import com.marineplay.chartplotter.ui.modules.camera.models.CameraPosition
import com.marineplay.chartplotter.ui.modules.camera.models.ViewMode

@Composable
fun CameraFeedGrid(
    selectedCamera: CameraPosition,
    viewMode: ViewMode,
    modifier: Modifier = Modifier
) {
    val activeCameras = when (viewMode) {
        ViewMode.SINGLE -> listOf(selectedCamera)
        ViewMode.DUAL -> listOf(CameraPosition.BOW, CameraPosition.STERN)
        ViewMode.QUAD -> CameraPosition.values().toList()
    }

    when (viewMode) {
        ViewMode.SINGLE -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(CameraTheme.backgroundColor)
            ) {
                CameraFeedItem(
                    position = selectedCamera,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        ViewMode.DUAL -> {
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .background(CameraTheme.backgroundColor)
            ) {
                activeCameras.forEach { position ->
                    CameraFeedItem(
                        position = position,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
        ViewMode.QUAD -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(CameraTheme.backgroundColor)
            ) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    activeCameras.take(2).forEach { position ->
                        CameraFeedItem(
                            position = position,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    activeCameras.drop(2).forEach { position ->
                        CameraFeedItem(
                            position = position,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

