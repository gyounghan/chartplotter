package com.marineplay.chartplotter.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.SavedPoint

@Composable
fun PointDeleteListDialog(
    points: List<SavedPoint>,
    onDeletePoint: (SavedPoint) -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf<SavedPoint?>(null) }
    
    // 삭제 확인 다이얼로그
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("포인트 삭제") },
            text = { 
                Text("'${showDeleteConfirm!!.name}' 포인트를 삭제하시겠습니까?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePoint(showDeleteConfirm!!)
                        showDeleteConfirm = null
                        onDismiss()
                    }
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("취소")
                }
            }
        )
    }
    
    // 포인트 목록 다이얼로그
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 삭제") },
        text = {
            if (points.isEmpty()) {
                Text("삭제할 포인트가 없습니다.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp)
                ) {
                    items(points) { point ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(point.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = point.name,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "위도: ${String.format("%.6f", point.latitude)}\n경도: ${String.format("%.6f", point.longitude)}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Button(
                                onClick = { showDeleteConfirm = point },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Text("삭제", color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

