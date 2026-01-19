package com.marineplay.chartplotter.ui.components.dialogs

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
fun SystemDeclinationDialog(
    currentMode: String,
    currentValue: Float,
    onModeChanged: (String) -> Unit,
    onValueChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(currentValue.toString()) }
    val modes = listOf("자동", "수동")
    
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
                    text = "헤딩값 설정",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                // 모드 선택
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "모드",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    modes.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (currentMode == mode) Color(0xFFE3F2FD) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onModeChanged(mode) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            if (currentMode == mode) {
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
                
                // 수동 모드일 때만 값 입력
                if (currentMode == "수동") {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() || char == '.' || char == '-' }) {
                                value = it
                            }
                        },
                        label = { Text("자기변량 값 (°)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소")
                    }
                    Button(
                        onClick = {
                            if (currentMode == "수동") {
                                val floatValue = value.toFloatOrNull() ?: currentValue
                                onValueChanged(floatValue)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("저장", color = Color.White)
                    }
                }
            }
        }
    }
}

