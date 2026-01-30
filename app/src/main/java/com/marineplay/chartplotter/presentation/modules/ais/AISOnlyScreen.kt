package com.marineplay.chartplotter.presentation.modules.ais

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.marineplay.chartplotter.presentation.modules.ais.components.SideNavigation
import com.marineplay.chartplotter.presentation.modules.ais.di.AISModule
import com.marineplay.chartplotter.presentation.modules.ais.models.AISTab
import com.marineplay.chartplotter.presentation.modules.ais.tabs.*

/**
 * AIS 전용 화면
 * 클린 아키텍처 기반으로 구현
 */
@Composable
fun AISOnlyScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { AISModule.createAISViewModel(context) }
    var selectedTab by remember { mutableStateOf(AISTab.RISK) }
    
    // 위치 서비스 초기화
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // USB 연결 시도 및 위치 업데이트 시작
    LaunchedEffect(Unit) {
        // 위치 권한 확인
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // 위치 요청 설정 (네트워크 위치도 사용)
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                5000L // 5초마다 업데이트
            ).apply {
                setMinUpdateDistanceMeters(10f)
                setWaitForAccurateLocation(false)
            }.build()
            
            // 위치 업데이트 요청
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationResult.lastLocation?.let { location ->
                            viewModel.updateLocation(
                                location.latitude,
                                location.longitude
                            )
                        }
                    }
                },
                android.os.Looper.getMainLooper()
            )
            
            // 즉시 마지막 알려진 위치 가져오기
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    viewModel.updateLocation(it.latitude, it.longitude)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AISTheme.backgroundColor)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 메인 콘텐츠 영역
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (selectedTab) {
                    AISTab.RISK -> RiskTab(viewModel = viewModel)
                    AISTab.VESSELS -> VesselsTab(viewModel = viewModel)
                    AISTab.EVENTS -> EventsTab(viewModel = viewModel)
                    AISTab.SETTINGS -> SettingsTab()
                }
            }

            // 우측 사이드 네비게이션
            SideNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.width(80.dp)
            )
        }
    }
}

