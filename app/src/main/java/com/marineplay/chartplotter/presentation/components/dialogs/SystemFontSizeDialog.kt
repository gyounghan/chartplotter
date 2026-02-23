package com.marineplay.chartplotter.presentation.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun SystemFontSizeDialog(
    currentSize: Float,
    onSizeChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val fontSizes = listOf(
        Pair(12f, "작게"),
        Pair(14f, "일반"),
        Pair(18f, "크게")
    )
    
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
                    text = "글자 크기 선택",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Text(
                    text = "원하는 크기를 선택하세요",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                fontSizes.forEach { (size, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSizeChanged(size)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            color = if (when (size) {
                            12f -> currentSize <= 13f
                            14f -> currentSize in 13f..15.5f
                            else -> currentSize >= 15.5f
                        }) Color(0xFF2196F3) else Color.Black
                        )
                        Text(
                            text = "${size.toInt()}sp",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("취소")
                }
            }
        }
    }
}

