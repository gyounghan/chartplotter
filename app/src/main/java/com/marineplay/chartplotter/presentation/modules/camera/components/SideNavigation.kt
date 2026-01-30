package com.marineplay.chartplotter.presentation.modules.camera.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.presentation.modules.camera.CameraTheme
import com.marineplay.chartplotter.presentation.modules.camera.models.CameraTab

@Composable
fun SideNavigation(
    selectedTab: CameraTab,
    onTabSelected: (CameraTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(CameraTheme.surfaceColor)
            .border(1.dp, CameraTheme.borderColor, RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        CameraTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(80.dp)
                    .clickable { onTabSelected(tab) }
                    .background(
                        if (isSelected) CameraTheme.primaryColor.copy(alpha = 0.2f) else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) CameraTheme.primaryColor else CameraTheme.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        color = if (isSelected) CameraTheme.primaryColor else CameraTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

