package com.marineplay.chartplotter.presentation.components

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

@Composable
fun PointDialog(
    showDialog: Boolean,
    pointName: String,
    selectedColor: Color,
    onNameChange: (String) -> Unit,
    onColorChange: (Color) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "포인트 등록",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = pointName,
                        onValueChange = onNameChange,
                        label = { Text("포인트명") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = "색상 선택",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val colors = listOf(
                            Color.Red, Color.Blue, Color.Green, Color.Yellow,
                            Color.Magenta, Color.Cyan, Color.Gray, Color.Black
                        )
                        
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = color,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onColorChange(color) }
                            ) {
                                if (selectedColor == color) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = Color.White.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                    enabled = pointName.isNotBlank()
                ) {
                    Text("등록")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        )
    }
}
