package com.marineplay.chartplotter.ui.modules.ais

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marineplay.chartplotter.ui.modules.ais.components.SideNavigation
import com.marineplay.chartplotter.ui.modules.ais.models.AISTab
import com.marineplay.chartplotter.ui.modules.ais.tabs.*

/**
 * AIS 전용 화면
 * Figma 디자인 기반으로 구현
 */
@Composable
fun AISOnlyScreen(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AISTab.RISK) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AISTheme.backgroundColor)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 메인 콘텐츠 영역
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (selectedTab) {
                    AISTab.RISK -> RiskTab()
                    AISTab.VESSELS -> VesselsTab()
                    AISTab.EVENTS -> EventsTab()
                    AISTab.SETTINGS -> SettingsTab()
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

