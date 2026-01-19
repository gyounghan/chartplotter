package com.marineplay.chartplotter.ui.modules.chart.components

import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.viewmodel.MainViewModel
import org.maplibre.android.maps.MapLibreMap

/**
 * 지도 컨트롤 버튼들 (줌 인/아웃, 메뉴)
 * 현재 위치 버튼은 Scaffold의 floatingActionButton으로 처리됨
 */
@Composable
fun MapControls(
    viewModel: MainViewModel,
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
                if (mapUiState.showCursor && !dialogUiState.isAddingWaypoint) {
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
                if (mapUiState.showCursor && dialogUiState.isAddingWaypoint) {
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
                if (mapUiState.showCursor && !dialogUiState.isAddingWaypoint) {
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
                    onClick = onZoomOut,
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
                    Text(
                        text = "-",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 줌 인 버튼
                FloatingActionButton(
                    onClick = onZoomIn,
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

