package com.kumhomarine.chartplotter.presentation.modules.ais.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumhomarine.chartplotter.presentation.modules.ais.AISTheme
import com.kumhomarine.chartplotter.presentation.modules.ais.models.AISTab

/**
 * 하단 네비게이션바 (AIS design 기준)
 * 모니터, 경계구역, 감시목록, 기록 탭
 */
@Composable
fun AISNavigationBar(
    selectedTab: AISTab,
    onTabSelected: (AISTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        AISTab.MONITOR,
        AISTab.GUARD,
        AISTab.WATCH,
        AISTab.LOG
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.Black)
            .drawBehind {
                drawLine(
                    color = AISTheme.borderColor,
                    strokeWidth = 2.dp.toPx(),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f)
                )
            }
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isActive = selectedTab == tab
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clickable { onTabSelected(tab) }
                        .background(
                            if (isActive) AISTheme.borderColor else AISTheme.cardBackgroundLight,
                            RoundedCornerShape(0.dp)
                        )
                        .then(
                            Modifier.border(
                                2.dp,
                                if (isActive) AISTheme.safe else AISTheme.borderColor,
                                RoundedCornerShape(0.dp)
                            )
                        )
                        .padding(horizontal = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 14.sp,
                        color = if (isActive) AISTheme.safe else AISTheme.textPrimary
                    )
                }
            }
        }
    }
}
