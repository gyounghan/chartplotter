package com.marineplay.chartplotter.presentation.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.SavedPoint

@Composable
fun PointEditDialog(
    point: SavedPoint,
    pointName: String,
    onPointNameChange: (String) -> Unit,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Red to "빨간색",
        Color.Blue to "파란색", 
        Color.Green to "초록색",
        Color.Yellow to "노란색",
        Color.Magenta to "자홍색",
        Color.Cyan to "청록색"
    )
    
    var showColorMenu by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 편집") },
        text = { 
            Column {
                // 좌표 표시
                Text("좌표:", fontSize = 14.sp)
                Text(
                    text = "위도: ${String.format("%.6f", point.latitude)}\n경도: ${String.format("%.6f", point.longitude)}",
                    modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 8.dp),
                    fontSize = 12.sp
                )
                
                // 포인트명 입력
                TextField(
                    value = pointName,
                    onValueChange = onPointNameChange,
                    label = { Text("포인트명") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                // 색상 선택
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("색상:", modifier = Modifier.padding(end = 8.dp))
                    Box(
                        modifier = Modifier
                            .background(selectedColor, CircleShape)
                            .clickable { showColorMenu = true }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .background(selectedColor, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = colors.find { it.first == selectedColor }?.second ?: "빨간색",
                        fontSize = 12.sp
                    )
                }
                
                // 색상 드롭다운 메뉴
                DropdownMenu(
                    expanded = showColorMenu,
                    onDismissRequest = { showColorMenu = false }
                ) {
                    colors.forEach { (color, name) ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(color, CircleShape)
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .background(color, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name)
                                }
                            },
                            onClick = {
                                onColorChange(color)
                                showColorMenu = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = pointName.isNotBlank()
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

