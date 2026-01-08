package com.marineplay.chartplotter.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.SavedPoint

@Composable
fun PointManageDialog(
    point: SavedPoint,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 관리") },
        text = { 
            Column {
                Text("포인트명: ${point.name}", fontSize = 16.sp)
                Text("위도: ${String.format("%.6f", point.latitude)}", fontSize = 14.sp)
                Text("경도: ${String.format("%.6f", point.longitude)}", fontSize = 14.sp)
                Text("등록일: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(point.timestamp))}", fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue
                )
            ) {
                Text("변경")
            }
        },
        dismissButton = {
            Row {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("삭제")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}

