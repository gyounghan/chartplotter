package com.marineplay.chartplotter.presentation.modules.chart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import org.maplibre.android.maps.MapLibreMap
import android.util.Log

/**
 * 지도 컨트롤 버튼들 (줌 인/아웃, 메뉴)
 * 현재 위치 버튼은 Scaffold의 floatingActionButton으로 처리됨
 */
@Composable
fun MapControls(
    viewModel: MainViewModel,
    isEditingRoute: Boolean = false,
    mapLibreMap: MapLibreMap?,
    locationManager: com.marineplay.chartplotter.LocationManager?,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCurrentLocation: () -> Unit,
    onAddWaypoint: () -> Unit,
    onCompleteWaypoint: () -> Unit,
    onNavigate: () -> Unit,
    onMenuClick: () -> Unit,
    onCreateQuickPoint: () -> Unit
) {
    val mapUiState = viewModel.mapUiState
    val pointUiState = viewModel.pointUiState
    val dialogUiState = viewModel.dialogUiState

    // UI 줌 버튼 상태 관리
    var isZoomInPressed by remember { mutableStateOf(false) }
    var isZoomOutPressed by remember { mutableStateOf(false) }

    // 🚀 UI 줌 인 버튼 롱클릭 반복 확대 (가속도 효과)
    LaunchedEffect(isZoomInPressed) {
        if (isZoomInPressed) {
            val pressStartTime = System.currentTimeMillis()
            var iteration = 0
            while (isZoomInPressed) {
                val elapsed = System.currentTimeMillis() - pressStartTime
                val zoomInSpeed = when {
                    elapsed < 500  -> 0.1   // 0~0.5초: 300ms
                    elapsed < 1500 -> 0.3   // 0.5~1.5초: 150ms
                    elapsed < 2500 -> 0.5    // 1.5~2.5초: 80ms
                    elapsed < 3500 -> 0.8    // 2.5~3.5초: 80ms
                    else           -> 1   // 3초 이상: 50ms
                }
                mapLibreMap?.let { map ->
                    val currentZoom = map.cameraPosition.zoom
                    val newZoom = (currentZoom + zoomInSpeed.toDouble()).coerceAtMost(22.0)
                    
                    // 커서가 있으면 커서 위치를 중앙으로 맞추고 줌 인
                    if (mapUiState.showCursor && mapUiState.cursorLatLng != null) {
                        val cursorLatLngValue = mapUiState.cursorLatLng!!
                        
                        // 커서 위치를 지도 중앙으로 즉시 이동하고 줌 인 (애니메이션 없이)
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                            org.maplibre.android.camera.CameraPosition.Builder()
                                .target(cursorLatLngValue)
                                .zoom(newZoom)
                                .build()
                        )
                        map.moveCamera(cameraUpdate) // animateCamera 대신 moveCamera 사용 (즉시 이동)
                        
                        // 커서 화면 위치를 중앙으로 업데이트
                        val centerScreenPoint = map.projection.toScreenLocation(cursorLatLngValue)
                        viewModel.updateCursorScreenPosition(centerScreenPoint)
                        
                        Log.d("[MapControls]", "줌 인: 커서 위치(${cursorLatLngValue.latitude}, ${cursorLatLngValue.longitude})를 중앙으로 맞추고 줌 $currentZoom -> $newZoom")
                    } else {
                        // 커서가 없으면 일반 줌 인
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                        map.animateCamera(cameraUpdate, 300)
                    }
                    Log.d("[MapControls]", "줌 인: $currentZoom -> $newZoom")
                    Log.d("[MapControls]", "iteration: $iteration")
                }
                
                // 시간 기반 가속도 효과: 누른 시간에 따라 인터벌 감소
                delay(100)
                iteration++
            }
        }
    }

    // 🚀 UI 줌 아웃 버튼 롱클릭 반복 축소 (가속도 효과)
    LaunchedEffect(isZoomOutPressed) {
        if (isZoomOutPressed) {
            val pressStartTime = System.currentTimeMillis()
            var iteration = 0
            while (isZoomOutPressed) {
                val elapsed = System.currentTimeMillis() - pressStartTime
                val zoomOutSpeed = when {
                    elapsed < 500  -> 0.1   // 0~0.5초: 300ms
                    elapsed < 1500 -> 0.3   // 0.5~1.5초: 150ms
                    elapsed < 2500 -> 0.5  // 1.5~2.5초: 80ms
                    elapsed < 3500 -> 0.8    // 1.5~3초: 80ms
                    else           -> 1    // 3초 이상: 50ms
                }
                mapLibreMap?.let { map ->
                   
                    val currentZoom = map.cameraPosition.zoom
                    val newZoom = (currentZoom - zoomOutSpeed.toDouble()).coerceAtLeast(6.0)
                    
                    // 커서가 있으면 커서 위치를 중앙으로 맞추고 줌 아웃
                    if (mapUiState.showCursor && mapUiState.cursorLatLng != null) {
                        val cursorLatLngValue = mapUiState.cursorLatLng!!
                        
                        // 커서 위치를 지도 중앙으로 즉시 이동하고 줌 아웃 (애니메이션 없이)
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                            org.maplibre.android.camera.CameraPosition.Builder()
                                .target(cursorLatLngValue)
                                .zoom(newZoom)
                                .build()
                        )
                        map.moveCamera(cameraUpdate) // animateCamera 대신 moveCamera 사용 (즉시 이동)
                        
                        // 커서 화면 위치를 중앙으로 업데이트
                        val centerScreenPoint = map.projection.toScreenLocation(cursorLatLngValue)
                        viewModel.updateCursorScreenPosition(centerScreenPoint)
                        
                        Log.d("[MapControls]", "줌 아웃: 커서 위치(${cursorLatLngValue.latitude}, ${cursorLatLngValue.longitude})를 중앙으로 맞추고 줌 $currentZoom -> $newZoom")
                    } else {
                        // 커서가 없으면 일반 줌 아웃
                        val cameraUpdate = org.maplibre.android.camera.CameraUpdateFactory.zoomTo(newZoom)
                        map.animateCamera(cameraUpdate, 300)
                    }
                    Log.d("[MapControls]", "줌 아웃: $currentZoom -> $newZoom")
                }
                
                // 시간 기반 가속도 효과: 누른 시간에 따라 인터벌 감소
                delay(100)
                iteration++
            }
        }
    }

    // 메뉴창이 열려있을 때는 플로팅 버튼 숨김
    if (!mapUiState.showMenu) {
        // 우측 상단: 메뉴 버튼 및 커서 관련 버튼들 (가로 배치)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 24.dp, end = 16.dp, start = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            // 클릭 가능한 영역을 보장하기 위한 투명한 레이어
            Spacer(modifier = Modifier.size(0.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 빠른 포인트 생성 버튼 (커서가 표시될 때만 보임, 경유지 추가 모드가 아닐 때)
                if (mapUiState.showCursor && !dialogUiState.isAddingWaypoint && !isEditingRoute) {
                    FloatingActionButton(
                        onClick = {
                            android.util.Log.d("[MapControls]", "빠른 포인트 생성 버튼 클릭됨")
                            try {
                                onCreateQuickPoint()
                            } catch (e: Exception) {
                                android.util.Log.e("[MapControls]", "빠른 포인트 생성 중 오류 발생: ${e.message}", e)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color(0xC6E0E0E0),
                        contentColor = Color.Black,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp,
                            focusedElevation = 4.dp,
                            hoveredElevation = 4.dp
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = "포인트 추가",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 경유지 추가 버튼 (커서가 표시될 때만 보임, 경유지 추가 모드일 때)
                if (mapUiState.showCursor && dialogUiState.isAddingWaypoint && !isEditingRoute) {
                    FloatingActionButton(
                        onClick = onAddWaypoint,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color(0xC6FFA500), // 주황색
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = "경유지 추가",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 경유지 추가 완료 버튼
                    FloatingActionButton(
                        onClick = onCompleteWaypoint,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color(0xC6FFA500), // 주황색
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "경유지 확인",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 항해 버튼 (커서가 표시될 때만 보임, 경유지 추가 모드가 아닐 때)
                if (mapUiState.showCursor && !dialogUiState.isAddingWaypoint && !isEditingRoute) {
                    FloatingActionButton(
                        onClick = onNavigate,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color(0xC6FFA500), // 주황색
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = "항해 시작",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (!isEditingRoute) {
                    // 메뉴 버튼
                    FloatingActionButton(
                        onClick = onMenuClick,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color(0xC6FFA500), // 주황색
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 가운데 하단: 줌 인/아웃 버튼
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 줌 아웃 버튼
                FloatingActionButton(
                    onClick = {
                        // 짧게 눌렀을 때 동작
                        onZoomOut()
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color(0xC6E2E2E2),
                    contentColor = Color.Black,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .border(
                            width = 1.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        // 손가락이 눌리는 순간
                                        isZoomOutPressed = true
                                        tryAwaitRelease() // 손을 뗄 때까지 대기
                                        // 손을 떼면 여기로 돌아옴
                                        isZoomOutPressed = false
                                    },
                                    onTap = {
                                        // 짧게 눌렀을 때는 onClick에서 처리됨
                                        Log.d("[MapControls]", "줌 아웃 짧게 클릭")
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 줌 인 버튼
                FloatingActionButton(
                    onClick = {
                        // 짧게 눌렀을 때 동작
                        onZoomIn()
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color(0xC6E2E2E2),
                    contentColor = Color.Black,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .border(
                            width = 1.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        // 손가락이 눌리는 순간
                                        isZoomInPressed = true
                                        tryAwaitRelease() // 손을 뗄 때까지 대기
                                        // 손을 떼면 여기로 돌아옴
                                        isZoomInPressed = false
                                    },
                                    onTap = {
                                        // 짧게 눌렀을 때는 onClick에서 처리됨
                                        Log.d("[MapControls]", "줌 인 짧게 클릭")
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

