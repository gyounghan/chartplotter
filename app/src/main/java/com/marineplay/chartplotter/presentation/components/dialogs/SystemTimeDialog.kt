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
fun SystemTimeDialog(
    currentTimeFormat: String,
    currentDateFormat: String,
    onTimeFormatChanged: (String) -> Unit,
    onDateFormatChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val timeFormats = listOf("24시간", "12시간")
    val dateFormats = listOf("YYYY-MM-DD", "MM/DD/YYYY", "DD/MM/YYYY")
    
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "시간 보정",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                // 표시형식
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "표시형식",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    timeFormats.forEach { format ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (currentTimeFormat == format) Color(0xFFE3F2FD) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onTimeFormatChanged(format) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = format,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            if (currentTimeFormat == format) {
                                Text(
                                    text = "✓",
                                    fontSize = 20.sp,
                                    color = Color(0xFF2196F3),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                // 날짜형식
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "날짜형식",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    dateFormats.forEach { format ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (currentDateFormat == format) Color(0xFFE3F2FD) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onDateFormatChanged(format) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = format,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            if (currentDateFormat == format) {
                                Text(
                                    text = "✓",
                                    fontSize = 20.sp,
                                    color = Color(0xFF2196F3),
                                    fontWeight = FontWeight.Bold
                                )
                            }
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

