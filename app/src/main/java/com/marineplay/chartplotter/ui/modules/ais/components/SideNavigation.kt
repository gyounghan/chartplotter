package com.marineplay.chartplotter.ui.modules.ais.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.ui.modules.ais.AISTheme
import com.marineplay.chartplotter.ui.modules.ais.models.AISTab

/**
 * 우측 사이드 네비게이션
 */
@Composable
fun SideNavigation(
    selectedTab: AISTab,
    onTabSelected: (AISTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        TabItem(AISTab.RISK, "위험", Icons.Default.Warning),
        TabItem(AISTab.VESSELS, "선박", Icons.Default.DirectionsBoat),
        TabItem(AISTab.EVENTS, "이벤트", Icons.Default.Event),
        TabItem(AISTab.SETTINGS, "설정", Icons.Default.Settings)
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(AISTheme.cardBackground)
            .border(
                width = 1.dp,
                color = AISTheme.borderColor,
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { tab ->
            NavigationItem(
                tab = tab,
                isSelected = selectedTab == tab.tab,
                onClick = { onTabSelected(tab.tab) }
            )
        }
    }
}

private data class TabItem(
    val tab: AISTab,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun NavigationItem(
    tab: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(72.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) AISTheme.cardBackgroundLight else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                modifier = Modifier.size(28.dp),
                tint = if (isSelected) AISTheme.primary else AISTheme.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tab.label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) AISTheme.primary else AISTheme.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

