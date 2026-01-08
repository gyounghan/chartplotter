package com.marineplay.chartplotter.ui.components.dialogs

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PointRegistrationDialog(
    centerCoordinates: String,
    pointName: String,
    onPointNameChange: (String) -> Unit,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    selectedIconType: String,
    onIconTypeChange: (String) -> Unit,
    getNextAvailablePointNumber: () -> Int,
    onRegister: () -> Unit,
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
    var focusState by remember { mutableStateOf("name") } // "name", "color", "register", "cancel"
    val focusRequester = remember { FocusRequester() }
    var isButtonPressed by remember { mutableStateOf(false) } // 버튼이 눌렸는지 추적
    var selectedColorIndex by remember { mutableStateOf(0) } // 색상 메뉴에서 선택된 색상 인덱스
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 등록") },
        text = { 
            Column {
                // 좌표 표시
                Text("현재 화면 중앙 좌표:", fontSize = 14.sp)
                Text(
                    text = centerCoordinates,
                    modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 8.dp),
                    fontSize = 12.sp
                )
                
                // 포인트명 입력 (자동 생성 + 편집 가능)
                val autoPointName = "Point${getNextAvailablePointNumber()}"
                val displayPointName = if (pointName.isBlank()) autoPointName else pointName
                
                TextField(
                    value = displayPointName,
                    onValueChange = onPointNameChange,
                    label = { Text("포인트명 (자동: $autoPointName)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(
                            if (focusState == "name") Color.Yellow.copy(alpha = 0.3f) else Color.Transparent,
                            androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                        .focusRequester(focusRequester)
                )
                
                // 색상 선택 (포커스 표시)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(
                            if (focusState == "color") Color.Yellow.copy(alpha = 0.3f) else Color.Transparent,
                            androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
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
                    onDismissRequest = { 
                        // 버튼이 눌렸거나 포커스가 color에 있을 때는 메뉴를 닫지 않음
                        if (!isButtonPressed && focusState != "color") {
                            showColorMenu = false
                        }
                        isButtonPressed = false // 버튼 상태 리셋
                    },
                    modifier = Modifier
                        .focusable()
                        .onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (e.nativeKeyEvent.keyCode) {
                                193 /* BUTTON_6  */ -> {
                                    // 색상 메뉴에서 이전 색상으로 이동
                                    selectedColorIndex = if (selectedColorIndex > 0) selectedColorIndex - 1 else colors.size - 1
                                    true
                                }
                                194 /* BUTTON_7  */ -> {
                                    // 색상 메뉴에서 다음 색상으로 이동
                                    selectedColorIndex = (selectedColorIndex + 1) % colors.size
                                    true
                                }
                                197 /* BUTTON_10 */ -> {
                                    // 현재 선택된 색상을 적용하고 메뉴 닫기
                                    onColorChange(colors[selectedColorIndex].first)
                                    showColorMenu = false
                                    Log.d("[Dialog]", "색상 선택됨: ${colors[selectedColorIndex].second}")
                                    true
                                }
                                198 /* BUTTON_11 */ -> {
                                    // 색상 메뉴 닫기
                                    showColorMenu = false
                                    true
                                }
                                else -> false
                            }
                        }
                ) {
                    colors.forEachIndexed { index, (color, name) ->
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
                                    Text(
                                        text = name,
                                        color = if (index == selectedColorIndex) Color.Blue else Color.Unspecified
                                    )
                                }
                            },
                            onClick = {
                                onColorChange(color)
                                showColorMenu = false
                            }
                        )
                    }
                }
                
                // 아이콘 선택
                Text("아이콘:", fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // 원 아이콘
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (selectedIconType == "circle") Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onIconTypeChange("circle") }
                            .border(
                                width = 2.dp,
                                color = if (selectedIconType == "circle") Color(0xFF2E7D32) else Color(0xFFBDBDBD),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color.Red, CircleShape)
                        )
                    }
                    
                    // 삼각형 아이콘
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (selectedIconType == "triangle") Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onIconTypeChange("triangle") }
                            .border(
                                width = 2.dp,
                                color = if (selectedIconType == "triangle") Color(0xFF2E7D32) else Color(0xFFBDBDBD),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▲",
                            fontSize = 20.sp,
                            color = Color.Red
                        )
                    }
                    
                    // 사각형 아이콘
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (selectedIconType == "square") Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onIconTypeChange("square") }
                            .border(
                                width = 2.dp,
                                color = if (selectedIconType == "square") Color(0xFF2E7D32) else Color(0xFFBDBDBD),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.Red, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRegister,
                enabled = pointName.isNotBlank(),
                modifier = Modifier.background(
                    if (focusState == "register") Color.Blue.copy(alpha = 0.3f) else Color.Transparent,
                    androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                )
            ) {
                Text("등록")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.background(
                    if (focusState == "cancel") Color.Red.copy(alpha = 0.3f) else Color.Transparent,
                    androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                )
            ) {
                Text("취소")
            }
        },
        // ⬇️ 여기서 193/194/195/196을 포커스 이동으로만 매핑
        modifier = Modifier
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.nativeKeyEvent.keyCode) {
                    193 /* BUTTON_6  */ -> {
                        // 색상 메뉴가 열려있으면 색상 선택, 아니면 포커스 위로 이동
                        if (showColorMenu) {
                            // 색상 메뉴에서 이전 색상으로 이동
                            selectedColorIndex = if (selectedColorIndex > 0) selectedColorIndex - 1 else colors.size - 1
                            Log.d("[Dialog]", "색상 메뉴에서 이전 색상: ${colors[selectedColorIndex].second}")
                        } else {
                            // 포커스 위로 이동
                            focusState = when (focusState) {
                                "name" -> "name"
                                "color" -> "name"
                                "register" -> "color"
                                "cancel" -> "register"
                                else -> "name"
                            }
                            Log.d("[Dialog]", "위로 이동: $focusState")
                        }
                        true
                    }
                    194 /* BUTTON_7  */ -> {
                        // 색상 메뉴가 열려있으면 색상 선택, 아니면 포커스 아래로 이동
                        Log.d("[Dialog]", "194 입력: ${showColorMenu}")
                        if (showColorMenu) {
                            // 색상 메뉴에서 다음 색상으로 이동
                            selectedColorIndex = (selectedColorIndex + 1) % colors.size
                            Log.d("[Dialog]", "색상 메뉴에서 다음 색상: ${colors[selectedColorIndex].second}")
                        } else {
                            // 포커스 아래로 이동
                            focusState = when (focusState) {
                                "name" -> "color"
                                "color" -> "register"
                                "register" -> "cancel"
                                "cancel" -> "cancel"
                                else -> "name"
                            }
                            Log.d("[Dialog]", "아래로 이동: $focusState")
                        }
                        true
                    }
                    195 /* BUTTON_8  */ -> {
                        // 포커스 왼쪽으로 이동
                        if (focusState == "register") {
                            focusState = "cancel"
                        } else if (focusState == "cancel") {
                            focusState = "register"
                        }
                        Log.d("[Dialog]", "좌로 이동: $focusState")
                        true
                    }
                    196 /* BUTTON_9  */ -> {
                        // 포커스 오른쪽으로 이동
                        if (focusState == "register") {
                            focusState = "cancel"
                        } else if (focusState == "cancel") {
                            focusState = "register"
                        }
                        Log.d("[Dialog]", "우로 이동: $focusState")
                        true
                    }
                    197 /* BUTTON_10 */ -> {
                        // 현재 포커스된 요소 선택/액션
                        isButtonPressed = true // 버튼이 눌렸음을 표시
                        if (showColorMenu) {
                            // 색상 메뉴가 열려있으면 현재 선택된 색상을 적용
                            onColorChange(colors[selectedColorIndex].first)
                            showColorMenu = false
                            Log.d("[Dialog]", "색상 선택됨: ${colors[selectedColorIndex].second}")
                        } else {
                            when (focusState) {
                                "name" -> {
                                    focusRequester.requestFocus()
                                    Log.d("[Dialog]", "포인트명 입력 필드 선택됨")
                                }
                                "color" -> {
                                    showColorMenu = true
                                    selectedColorIndex = colors.indexOfFirst { it.first == selectedColor }.takeIf { it >= 0 } ?: 0
                                    Log.d("[Dialog]", "색상 메뉴 열림: $showColorMenu")
                                }
                                "register" -> {
                                    if (pointName.isNotBlank()) {
                                        onRegister()
                                    }
                                    Log.d("[Dialog]", "등록 버튼 클릭됨")
                                }
                                "cancel" -> {
                                    onDismiss()
                                    Log.d("[Dialog]", "취소 버튼 클릭됨")
                                }
                            }
                        }
                        true
                    }
                    198 /* BUTTON_11 */ -> {
                        // 취소
                        if (showColorMenu) {
                            showColorMenu = false
                        } else {
                            onDismiss()
                        }
                        Log.d("[Dialog]", "취소")
                        true
                    }
                    else -> false
                }
            }
    )
}

