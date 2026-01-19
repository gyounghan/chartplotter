package com.marineplay.chartplotter.ui.modules.chart.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marineplay.chartplotter.*
import com.marineplay.chartplotter.SavedPoint
import com.marineplay.chartplotter.viewmodel.MainViewModel
import org.maplibre.android.maps.MapLibreMap

/**
 * 메뉴 패널 컴포넌트
 */
@Composable
fun MenuPanel(
    viewModel: MainViewModel,
    mapLibreMap: MapLibreMap?,
    locationManager: LocationManager?,
    loadPointsFromLocal: () -> List<SavedPoint>,
    getNextAvailablePointNumber: () -> Int,
    updateMapRotation: () -> Unit,
    stopTrackRecording: () -> Unit
) {
    val mapUiState = viewModel.mapUiState
    val trackUiState = viewModel.trackUiState

    if (mapUiState.showMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .background(Color.DarkGray)
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { /* 메뉴창 내부 클릭 시 지도 클릭 이벤트 차단 */ }
                    }
            ) {
                Column {
                    // 메뉴 헤더 (제목 + 닫기/뒤로가기 버튼)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (mapUiState.currentMenu) {
                                "main" -> "메뉴"
                                "point" -> "포인트"
                                "ais" -> "AIS"
                                "navigation" -> "항해"
                                "track" -> "항적"
                                "display" -> "화면표시"
                                "system" -> "시스템"
                                "system_language" -> "사용언어"
                                "system_vessel" -> "자선설정"
                                "system_font" -> "문자크기"
                                "system_volume" -> "음량"
                                "system_time" -> "시간"
                                "system_geodetic" -> "측지계"
                                "system_coordinates" -> "위경도 표시"
                                "system_declination" -> "자기변량"
                                "system_reset" -> "기본설정으로 복원"
                                "system_power" -> "전원제어"
                                "system_advanced" -> "고급"
                                "system_connection" -> "연결 및 등록"
                                "system_info" -> "정보"
                                else -> if (mapUiState.currentMenu.startsWith("system_")) "시스템" else "메뉴"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(
                            onClick = {
                                when {
                                    mapUiState.currentMenu == "main" -> {
                                        viewModel.updateShowMenu(false)
                                        viewModel.updateCurrentMenu("main")
                                    }
                                    mapUiState.currentMenu.startsWith("system_") -> {
                                        viewModel.updateCurrentMenu("system")
                                    }
                                    else -> {
                                        viewModel.updateCurrentMenu("main")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (mapUiState.currentMenu == "main") Icons.Default.Close else Icons.Default.ArrowBack,
                                contentDescription = if (mapUiState.currentMenu == "main") "메뉴 닫기" else "뒤로가기",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 메인 메뉴
                    if (mapUiState.currentMenu == "main") {
                        MenuMainContent(viewModel)
                    }

                    // 포인트 메뉴
                    if (mapUiState.currentMenu == "point") {
                        MenuPointContent(
                            viewModel = viewModel,
                            mapLibreMap = mapLibreMap,
                            loadPointsFromLocal = loadPointsFromLocal,
                            getNextAvailablePointNumber = getNextAvailablePointNumber
                        )
                    }

                    // 항해 메뉴
                    if (mapUiState.currentMenu == "navigation") {
                        MenuNavigationContent(
                            viewModel = viewModel,
                            mapLibreMap = mapLibreMap,
                            loadPointsFromLocal = loadPointsFromLocal
                        )
                    }

                    // 항적 메뉴
                    if (mapUiState.currentMenu == "track") {
                        MenuTrackContent(
                            viewModel = viewModel,
                            trackUiState = trackUiState,
                            stopTrackRecording = stopTrackRecording
                        )
                    }

                    // AIS 메뉴
                    if (mapUiState.currentMenu == "ais") {
                        MenuAisContent(viewModel)
                    }

                    // 화면표시 방법설정 메뉴
                    if (mapUiState.currentMenu == "display") {
                        MenuDisplayContent(
                            viewModel = viewModel,
                            mapUiState = mapUiState,
                            loadPointsFromLocal = loadPointsFromLocal,
                            updateMapRotation = updateMapRotation
                        )
                    }

                    // 시스템 메뉴
                    if (mapUiState.currentMenu == "system") {
                        MenuSystemContent(viewModel)
                    }

                    // 시스템 하위 메뉴들
                    if (mapUiState.currentMenu.startsWith("system_")) {
                        MenuSystemSubContent(viewModel, mapUiState.currentMenu)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuMainContent(viewModel: MainViewModel) {
    Text(
        "포인트",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("point") },
        color = Color.White
    )
    Text(
        "항해",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("navigation") },
        color = Color.White
    )
    Text(
        "항적",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("track") },
        color = Color.White
    )
    Text(
        "AIS",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("ais") },
        color = Color.White
    )
    Text(
        "화면표시 방법설정",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { viewModel.updateCurrentMenu("display") },
        color = Color.White
    )
    Text(
        "고급",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowSettingsScreen(true)
                viewModel.updateShowMenu(false)
            },
        color = Color.White
    )
}

@Composable
private fun MenuPointContent(
    viewModel: MainViewModel,
    mapLibreMap: MapLibreMap?,
    loadPointsFromLocal: () -> List<SavedPoint>,
    getNextAvailablePointNumber: () -> Int
) {
    val mapUiState = viewModel.mapUiState
    
    Text(
        "포인트 생성",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                val targetLatLng =
                    if (mapUiState.showCursor && mapUiState.cursorLatLng != null) {
                        mapUiState.cursorLatLng
                    } else {
                        mapLibreMap?.cameraPosition?.target
                    }

                targetLatLng?.let { latLng ->
                    viewModel.updateCurrentLatLng(latLng)
                    viewModel.updateCenterCoordinates(
                        "위도: ${String.format("%.6f", latLng.latitude)}\n경도: ${String.format("%.6f", latLng.longitude)}"
                    )
                    viewModel.updatePointName("Point${getNextAvailablePointNumber()}")
                    viewModel.updateSelectedColor(Color.Red)
                } ?: run {
                    viewModel.updateCenterCoordinates("좌표를 가져올 수 없습니다.")
                    viewModel.updateCurrentLatLng(null)
                }
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
                viewModel.updateShowDialog(true)
            },
        color = Color.White
    )
    Text(
        "포인트 삭제",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
                viewModel.updateShowPointDeleteList(true)
            },
        color = Color.White
    )
    Text(
        "포인트 변경",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
                // TODO: 포인트 변경 화면 구현
            },
        color = Color.White
    )
}

@Composable
private fun MenuNavigationContent(
    viewModel: MainViewModel,
    mapLibreMap: MapLibreMap?,
    loadPointsFromLocal: () -> List<SavedPoint>
) {
    val mapUiState = viewModel.mapUiState
    
    Text(
        "항해 시작",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                val savedPoints = loadPointsFromLocal()
                if (savedPoints.isNotEmpty()) {
                    viewModel.updateShowPointSelectionDialog(true)
                } else {
                    Log.d("[MenuPanel]", "저장된 포인트가 없어서 항해를 시작할 수 없습니다.")
                }
            },
        color = Color.White
    )
    Text(
        "목적지 변경",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                val savedPoints = loadPointsFromLocal()
                if (savedPoints.isNotEmpty()) {
                    viewModel.updateShowPointSelectionDialog(true)
                } else {
                    Log.d("[MenuPanel]", "저장된 포인트가 없어서 목적지를 변경할 수 없습니다.")
                }
            },
        color = Color.White
    )
    Text(
        "경유지 관리",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowWaypointDialog(true)
            },
        color = Color.White
    )
    Text(
        "항해 중지",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateMapDisplayMode("노스업")
                viewModel.updateCoursePoint(null)
                viewModel.updateNavigationPoint(null)
                viewModel.updateWaypoints(emptyList())
                mapLibreMap?.let { map ->
                    PMTilesLoader.removeNavigationLine(map)
                    PMTilesLoader.removeNavigationMarker(map)
                }
                viewModel.updateCurrentMenu("main")
            },
        color = Color.White
    )
    
    if (mapUiState.mapDisplayMode == "코스업" && (mapUiState.coursePoint != null || mapUiState.navigationPoint != null)) {
        Text(
            text = "항해 중: ${mapUiState.coursePoint?.name ?: mapUiState.navigationPoint?.name ?: "커서 위치"}",
            fontSize = 14.sp,
            color = Color.Yellow,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun MenuTrackContent(
    viewModel: MainViewModel,
    trackUiState: com.marineplay.chartplotter.viewmodel.TrackUiState,
    stopTrackRecording: () -> Unit
) {
    Text(
        "항적 설정",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowTrackSettingsDialog(true)
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
            },
        color = Color.White
    )
    Text(
        "항적 목록",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowTrackListDialog(true)
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
            },
        color = Color.White
    )
    
    if (trackUiState.isRecordingTrack && trackUiState.currentRecordingTrack != null) {
        Text(
            "항적 기록 중지",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    stopTrackRecording()
                    viewModel.updateShowMenu(false)
                    viewModel.updateCurrentMenu("main")
                },
            color = Color.Red
        )
        Text(
            text = "기록 중: ${trackUiState.currentRecordingTrack!!.name}",
            fontSize = 14.sp,
            color = Color.Yellow,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun MenuAisContent(viewModel: MainViewModel) {
    Text(
        "AIS ON/OFF",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
                // TODO: AIS ON/OFF 구현
            },
        color = Color.White
    )
    Text(
        "AIS 설정",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
                // TODO: AIS 설정 화면 구현
            },
        color = Color.White
    )
}

@Composable
private fun MenuDisplayContent(
    viewModel: MainViewModel,
    mapUiState: com.marineplay.chartplotter.viewmodel.MapUiState,
    loadPointsFromLocal: () -> List<SavedPoint>,
    updateMapRotation: () -> Unit
) {
    Text(
        "노스업",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                Log.d("[MenuPanel]", "지도 표시 모드 변경: ${mapUiState.mapDisplayMode} -> 노스업")
                viewModel.updateMapDisplayMode("노스업")
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
            },
        color = if (mapUiState.mapDisplayMode == "노스업") Color.Yellow else Color.White
    )
    Text(
        "헤딩업",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                Log.d("[MenuPanel]", "지도 표시 모드 변경: ${mapUiState.mapDisplayMode} -> 헤딩업")
                viewModel.updateMapDisplayMode("헤딩업")
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
            },
        color = if (mapUiState.mapDisplayMode == "헤딩업") Color.Yellow else Color.White
    )
    Text(
        "코스업",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                Log.d("[MenuPanel]", "지도 표시 모드 변경: ${mapUiState.mapDisplayMode} -> 코스업")
                
                if (mapUiState.navigationPoint != null) {
                    viewModel.updateCoursePoint(mapUiState.navigationPoint)
                    viewModel.updateMapDisplayMode("코스업")
                    updateMapRotation()
                    Log.d("[MenuPanel]", "항해 포인트를 코스업으로 적용: ${mapUiState.navigationPoint!!.name}")
                } else {
                    val savedPoints = loadPointsFromLocal()
                    if (savedPoints.isNotEmpty()) {
                        viewModel.updateShowPointSelectionDialog(true)
                    } else {
                        android.util.Log.d("[MenuPanel]", "코스업을 위해 포인트를 먼저 생성하세요")
                    }
                }
                viewModel.updateShowMenu(false)
                viewModel.updateCurrentMenu("main")
            },
        color = if (mapUiState.mapDisplayMode == "코스업") Color.Yellow else Color.White
    )
}

@Composable
private fun MenuSystemContent(viewModel: MainViewModel) {
    Text(
        "사용언어",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_language")
            },
        color = Color.White
    )
    Text(
        "자선설정",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_vessel")
            },
        color = Color.White
    )
    Text(
        "문자크기",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_font")
            },
        color = Color.White
    )
    Text(
        "음량",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_volume")
            },
        color = Color.White
    )
    Text(
        "시간",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_time")
            },
        color = Color.White
    )
    Text(
        "측지계",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_geodetic")
            },
        color = Color.White
    )
    Text(
        "위경도 표시",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_coordinates")
            },
        color = Color.White
    )
    Text(
        "자기변량",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_declination")
            },
        color = Color.White
    )
    Text(
        "기본설정으로 복원",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_reset")
            },
        color = Color.White
    )
    Text(
        "전원제어",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_power")
            },
        color = Color.White
    )
    Text(
        "고급",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_advanced")
            },
        color = Color.White
    )
    Text(
        "연결 및 등록",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_connection")
            },
        color = Color.White
    )
    Text(
        "정보",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                viewModel.updateCurrentMenu("system_info")
            },
        color = Color.White
    )
}

@Composable
private fun MenuSystemSubContent(viewModel: MainViewModel, currentSubMenu: String) {
    when (currentSubMenu) {
        "system_language" -> {
            val languages = listOf("한국어", "영어", "일본어", "중국어")
            val currentLanguage = viewModel.systemSettings.language
            
            languages.forEach { language ->
                Text(
                    language,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            viewModel.updateLanguage(language)
                            viewModel.updateShowMenu(false)
                            viewModel.updateCurrentMenu("main")
                        },
                    color = if (currentLanguage == language) Color.Yellow else Color.White
                )
            }
        }
        "system_vessel" -> {
            Text(
                "자선설정",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        viewModel.updateShowVesselSettingsDialog(true)
                        viewModel.updateShowMenu(false)
                        viewModel.updateCurrentMenu("main")
                    },
                color = Color.White
            )
        }
        "system_font" -> {
            val fontSizes = listOf(
                Pair(12f, "작은 글자"),
                Pair(14f, "보통 글자"),
                Pair(16f, "큰 글자"),
                Pair(18f, "매우 큰 글자"),
                Pair(20f, "특대 글자")
            )
            val currentSize = viewModel.systemSettings.fontSize
            
            fontSizes.forEach { (size, label) ->
                Text(
                    label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            viewModel.updateFontSize(size)
                            viewModel.updateShowMenu(false)
                            viewModel.updateCurrentMenu("main")
                        },
                    color = if (kotlin.math.abs(currentSize - size) < 0.5f) Color.Yellow else Color.White
                )
            }
        }
        "system_volume" -> {
            val volumeLevels = listOf(0, 25, 50, 75, 100)
            val currentVolume = viewModel.systemSettings.buttonVolume
            
            volumeLevels.forEach { volume ->
                Text(
                    "$volume%",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            viewModel.updateButtonVolume(volume)
                            viewModel.updateShowMenu(false)
                            viewModel.updateCurrentMenu("main")
                        },
                    color = if (kotlin.math.abs(currentVolume - volume) <= 12) Color.Yellow else Color.White
                )
            }
        }
        "system_time" -> {
            val timeFormats = listOf("24시간", "12시간")
            val dateFormats = listOf("YYYY-MM-DD", "MM/DD/YYYY", "DD/MM/YYYY")
            val currentTimeFormat = viewModel.systemSettings.timeFormat
            val currentDateFormat = viewModel.systemSettings.dateFormat
            
            Text(
                "표시형식",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            )
            timeFormats.forEach { format ->
                Text(
                    format,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            viewModel.updateTimeFormat(format)
                            viewModel.updateShowMenu(false)
                            viewModel.updateCurrentMenu("main")
                        },
                    color = if (currentTimeFormat == format) Color.Yellow else Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "날짜형식",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            )
            dateFormats.forEach { format ->
                Text(
                    format,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            viewModel.updateDateFormat(format)
                            viewModel.updateShowMenu(false)
                            viewModel.updateCurrentMenu("main")
                        },
                    color = if (currentDateFormat == format) Color.Yellow else Color.White
                )
            }
        }
        "system_geodetic" -> {
            val geodeticSystems = listOf("WGS84", "GRS80", "Bessel", "Tokyo", "PZ-90")
            val currentSystem = viewModel.systemSettings.geodeticSystem
            
            geodeticSystems.forEach { system ->
                Text(
                    system,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            viewModel.updateGeodeticSystem(system)
                            viewModel.updateShowMenu(false)
                            viewModel.updateCurrentMenu("main")
                        },
                    color = if (currentSystem == system) Color.Yellow else Color.White
                )
            }
        }
        "system_coordinates" -> {
            val coordinateFormats = listOf("도", "도분", "도분초")
            val currentFormat = viewModel.systemSettings.coordinateFormat
            
            coordinateFormats.forEach { format ->
                Text(
                    format,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            viewModel.updateCoordinateFormat(format)
                            viewModel.updateShowMenu(false)
                            viewModel.updateCurrentMenu("main")
                        },
                    color = if (currentFormat == format) Color.Yellow else Color.White
                )
            }
        }
        "system_declination" -> {
            val declinationModes = listOf("자동", "수동")
            val currentMode = viewModel.systemSettings.declinationMode
            
            declinationModes.forEach { mode ->
                Text(
                    mode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            if (mode == "수동") {
                                // 수동 모드일 때는 다이얼로그로 값 입력
                                viewModel.updateShowDeclinationDialog(true)
                                viewModel.updateShowMenu(false)
                                viewModel.updateCurrentMenu("main")
                            } else {
                                // 자동 모드일 때는 바로 설정
                                viewModel.updateDeclinationMode(mode)
                                viewModel.updateShowMenu(false)
                                viewModel.updateCurrentMenu("main")
                            }
                        },
                    color = if (currentMode == mode) Color.Yellow else Color.White
                )
            }
        }
        "system_reset" -> {
            Text(
                "설정 초기화",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        viewModel.updateShowResetConfirmDialog(true)
                        viewModel.updateShowMenu(false)
                        viewModel.updateCurrentMenu("main")
                    },
                color = Color.White
            )
        }
        "system_power" -> {
            val pingSync = viewModel.systemSettings.pingSync
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "핑 동기화",
                    color = Color.White
                )
                Switch(
                    checked = pingSync,
                    onCheckedChange = {
                        viewModel.updatePingSync(it)
                    }
                )
            }
        }
        "system_advanced" -> {
            val defaultFeatures = listOf(
                "GPS 자동 업데이트",
                "AIS 자동 수신",
                "항적 자동 저장",
                "경보 음성 알림",
                "야간 모드",
                "배터리 절약 모드"
            )
            val advancedFeatures = viewModel.systemSettings.advancedFeatures
            
            defaultFeatures.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        feature,
                        color = Color.White
                    )
                    Switch(
                        checked = advancedFeatures[feature] ?: false,
                        onCheckedChange = {
                            viewModel.updateAdvancedFeature(feature, it)
                        }
                    )
                }
            }
        }
        "system_connection" -> {
            val isConnected = viewModel.systemSettings.mobileConnected
            
            if (isConnected) {
                Text(
                    "연결 해제",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            val newSettings = viewModel.systemSettings.copy(mobileConnected = false)
                            viewModel.updateSystemSettings(newSettings)
                            viewModel.updateShowMenu(false)
                            viewModel.updateCurrentMenu("main")
                        },
                    color = Color.Red
                )
            } else {
                Text(
                    "연결",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            val newSettings = viewModel.systemSettings.copy(mobileConnected = true)
                            viewModel.updateSystemSettings(newSettings)
                            viewModel.updateShowMenu(false)
                            viewModel.updateCurrentMenu("main")
                        },
                    color = Color(0xFF4CAF50)
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "연결 상태",
                    color = Color.White
                )
                Text(
                    if (isConnected) "연결됨" else "연결 안됨",
                    color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        "system_info" -> {
            Text(
                "SW 정보",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        viewModel.updateShowInfoDialog(true)
                        viewModel.updateShowMenu(false)
                        viewModel.updateCurrentMenu("main")
                    },
                color = Color.White
            )
        }
    }
}

