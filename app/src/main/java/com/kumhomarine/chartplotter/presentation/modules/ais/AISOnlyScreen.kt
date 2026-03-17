package com.kumhomarine.chartplotter.presentation.modules.ais

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kumhomarine.chartplotter.presentation.modules.ais.components.AISNavigationBar
import com.kumhomarine.chartplotter.presentation.modules.ais.components.AISStatusBar
import com.kumhomarine.chartplotter.presentation.modules.ais.di.AISModule
import com.kumhomarine.chartplotter.presentation.modules.ais.models.AISTab
import com.kumhomarine.chartplotter.presentation.modules.ais.tabs.GuardTab
import com.kumhomarine.chartplotter.presentation.modules.ais.tabs.LogTab
import com.kumhomarine.chartplotter.presentation.modules.ais.tabs.MonitorTab
import com.kumhomarine.chartplotter.presentation.modules.ais.tabs.WatchTab

/**
 * AIS 전용 화면
 * AIS design 폴더 기준 레이아웃 및 디자인 적용
 */
@Composable
fun AISOnlyScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { AISModule.createAISViewModel(context) }
    val isConnected by viewModel.isConnected.collectAsState()
    var selectedTab by remember { mutableStateOf(AISTab.MONITOR) }
    
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AISTheme.backgroundColor)
    ) {
        AISStatusBar(
            gpsFix = true,
            aisRx = isConnected,
            nmea = true
        )
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                AISTab.MONITOR -> MonitorTab(viewModel = viewModel)
                AISTab.GUARD -> GuardTab()
                AISTab.WATCH -> WatchTab(viewModel = viewModel)
                AISTab.LOG -> LogTab(viewModel = viewModel)
            }
        }
        AISNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}

