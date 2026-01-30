package com.marineplay.chartplotter.presentation.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun SystemGeodeticDialog(
    currentSystem: String,
    onSystemSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val systems = listOf("WGS84", "GRS80", "Bessel", "Tokyo", "PZ-90")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "측지계 설정",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                systems.forEach { system ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (currentSystem == system) Color(0xFFE3F2FD) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSystemSelected(system) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = system,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        if (currentSystem == system) {
                            Text(
                                text = "✓",
                                fontSize = 20.sp,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("확인", color = Color.White)
                }
            }
        }
    }
}

