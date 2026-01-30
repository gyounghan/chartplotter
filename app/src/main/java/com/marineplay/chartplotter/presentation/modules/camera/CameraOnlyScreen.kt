package com.marineplay.chartplotter.presentation.modules.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marineplay.chartplotter.presentation.modules.camera.components.SideNavigation
import com.marineplay.chartplotter.presentation.modules.camera.models.CameraPosition
import com.marineplay.chartplotter.presentation.modules.camera.models.CameraTab
import com.marineplay.chartplotter.presentation.modules.camera.models.ViewMode
import com.marineplay.chartplotter.presentation.modules.camera.tabs.*

/**
 * 카메라 전용 화면
 * Figma 디자인 기반으로 구현
 */
@Composable
fun CameraOnlyScreen(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(CameraTab.LIVE) }
    var selectedCamera by remember { mutableStateOf(CameraPosition.BOW) }
    var viewMode by remember { mutableStateOf(ViewMode.SINGLE) }
    var isRecording by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CameraTheme.backgroundColor)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 메인 콘텐츠 영역
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (selectedTab) {
                    CameraTab.LIVE -> LiveViewTab(
                        selectedCamera = selectedCamera,
                        viewMode = viewMode,
                        isRecording = isRecording,
                        onCameraSelected = { selectedCamera = it },
                        onViewModeChanged = { viewMode = it },
                        onRecordingChanged = { isRecording = it }
                    )
                    CameraTab.RECORDINGS -> RecordingsTab()
                    CameraTab.CAMERAS -> CameraManagementTab()
                    CameraTab.SETTINGS -> SettingsTab()
                }
            }

            // 우측 사이드 네비게이션
            SideNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.width(80.dp)
            )
        }
    }
}

