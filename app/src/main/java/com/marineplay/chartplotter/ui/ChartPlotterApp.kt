package com.marineplay.chartplotter.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.marineplay.chartplotter.helpers.PointHelper
import com.marineplay.chartplotter.viewmodel.MainViewModel
import com.marineplay.chartplotter.LocationManager
import com.marineplay.chartplotter.EntryMode
import com.marineplay.chartplotter.ui.modules.*
import com.marineplay.chartplotter.ui.modules.chart.ChartOnlyScreen
import com.marineplay.chartplotter.ui.modules.ais.AISOnlyScreen
import com.marineplay.chartplotter.ui.modules.camera.CameraOnlyScreen
import org.maplibre.android.maps.MapLibreMap

/**
 * ChartPlotterApp - EntryMode에 따라 화면을 구성하는 메인 진입점
 * 
 * 이 함수는 MainActivity에서 호출되며, EntryMode에 따라 적절한 화면을 표시합니다.
 * 
 * Intent Contract:
 * - Action: android.intent.action.MAIN
 * - Component: com.marineplay.chartplotter.MainActivity
 * - Extra Key: ENTRY_MODE
 * - Extra Type: String (CHART_ONLY, BLACKBOX_ONLY, CAMERA_ONLY, AIS_ONLY, DASHBOARD_ONLY, SPLIT)
 */
@Composable
fun ChartPlotterApp(
    entryMode: EntryMode,
    viewModel: MainViewModel,
    activity: ComponentActivity,
    pointHelper: PointHelper,
    onMapLibreMapChange: (MapLibreMap?) -> Unit = {},
    onLocationManagerChange: (LocationManager?) -> Unit = {}
) {
    // CHART_ONLY가 아닌 경우 뒤로가기 시 앱 종료
    if (entryMode != EntryMode.CHART_ONLY) {
        BackHandler(enabled = true) {
            android.util.Log.d("[ChartPlotterApp]", "뒤로가기: $entryMode 모드에서 앱 종료")
            // finishAffinity()를 사용하여 Task 전체를 종료
            activity.finishAffinity()
        }
    }
    
    when (entryMode) {
        EntryMode.CHART_ONLY -> {
            // 차트 전용 화면
            ChartOnlyScreen(
                viewModel = viewModel,
                activity = activity,
                pointHelper = pointHelper,
                onMapLibreMapChange = onMapLibreMapChange,
                onLocationManagerChange = onLocationManagerChange
            )
        }
        EntryMode.BLACKBOX_ONLY -> {
            // 블랙박스 전용 화면
            BlackboxOnlyScreen()
        }
        EntryMode.CAMERA_ONLY -> {
            // 카메라 전용 화면
            CameraOnlyScreen()
        }
        EntryMode.AIS_ONLY -> {
            // AIS 전용 화면
            AISOnlyScreen()
        }
        EntryMode.DASHBOARD_ONLY -> {
            // 계기판 전용 화면
            DashboardOnlyScreen()
        }
        EntryMode.SPLIT -> {
            // 화면 분할 (차트 + 블랙박스) (향후 구현)
            // TODO: SplitScreen 구현
            ChartOnlyScreen(
                viewModel = viewModel,
                activity = activity,
                pointHelper = pointHelper,
                onMapLibreMapChange = onMapLibreMapChange,
                onLocationManagerChange = onLocationManagerChange
            )
        }
    }
}

