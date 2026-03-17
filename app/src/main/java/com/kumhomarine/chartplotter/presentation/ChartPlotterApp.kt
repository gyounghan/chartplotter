package com.kumhomarine.chartplotter.presentation

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.kumhomarine.chartplotter.presentation.viewmodel.MainViewModel
import com.kumhomarine.chartplotter.presentation.viewmodel.SettingsViewModel
import com.kumhomarine.chartplotter.presentation.viewmodel.TrackViewModel
import com.kumhomarine.chartplotter.presentation.viewmodel.RouteViewModel
import com.kumhomarine.chartplotter.LocationManager
import com.kumhomarine.chartplotter.EntryMode
import com.kumhomarine.chartplotter.presentation.modules.*
import com.kumhomarine.chartplotter.presentation.modules.chart.ChartOnlyScreen
import com.kumhomarine.chartplotter.domain.repositories.PointRepository
import com.kumhomarine.chartplotter.presentation.modules.ais.AISOnlyScreen
import com.kumhomarine.chartplotter.presentation.modules.camera.CameraOnlyScreen
import com.kumhomarine.chartplotter.presentation.modules.destination.DestinationListScreen
import com.kumhomarine.chartplotter.presentation.modules.track.TrackListScreen
import com.kumhomarine.chartplotter.presentation.modules.route.RouteListScreen
import com.kumhomarine.chartplotter.domain.repositories.TrackRepository
import com.kumhomarine.chartplotter.domain.repositories.RouteRepository
import org.maplibre.android.maps.MapLibreMap

/**
 * ChartPlotterApp - EntryMode에 따라 화면을 구성하는 메인 진입점
 * 
 * 이 함수는 MainActivity에서 호출되며, EntryMode에 따라 적절한 화면을 표시합니다.
 * 
 * Intent Contract:
 * - Action: android.intent.action.MAIN
 * - Component: com.kumhomarine.chartplotter.MainActivity
 * - Extra Key: ENTRY_MODE
 * - Extra Type: String (CHART_ONLY, BLACKBOX_ONLY, CAMERA_ONLY, AIS_ONLY, SPLIT)
 */
@Composable
fun ChartPlotterApp(
    entryMode: EntryMode,
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    trackViewModel: TrackViewModel,
    routeViewModel: RouteViewModel,
    pointRepository: PointRepository,
    trackRepository: TrackRepository,
    routeRepository: RouteRepository,
    activity: ComponentActivity,
    onMapLibreMapChange: (MapLibreMap?) -> Unit = {},
    onLocationManagerChange: (LocationManager?) -> Unit = {}
) {
    // CHART_ONLY가 아닌 경우 뒤로가기 시 Launcher로 복귀
    if (entryMode != EntryMode.CHART_ONLY) {
        BackHandler(enabled = true) {
            android.util.Log.d("[ChartPlotterApp]", "뒤로가기: $entryMode 모드에서 앱 종료")
            activity.finish()
        }
    }
    
    when (entryMode) {
        EntryMode.CHART_ONLY -> {
            // 차트 전용 화면
            ChartOnlyScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                trackViewModel = trackViewModel,
                routeViewModel = routeViewModel,
                activity = activity,
                onMapLibreMapChange = onMapLibreMapChange,
                onLocationManagerChange = onLocationManagerChange
            )
            // AISOnlyScreen()
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
        EntryMode.DESTINATION_LIST -> {
            // 목적지 리스트 화면
            DestinationListScreen(
                pointRepository = pointRepository,
                activity = activity
            )
        }
        EntryMode.TRACK_LIST -> {
            // 항적 리스트 화면
            TrackListScreen(
                trackRepository = trackRepository,
                activity = activity
            )
        }
        EntryMode.ROUTE_LIST -> {
            // 경로 리스트 화면
            RouteListScreen(
                routeRepository = routeRepository,
                activity = activity
            )
        }
        EntryMode.SPLIT -> {
            // 화면 분할 (차트 + 블랙박스) (향후 구현)
            // TODO: SplitScreen 구현
            ChartOnlyScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                trackViewModel = trackViewModel,
                routeViewModel = routeViewModel,
                activity = activity,
                onMapLibreMapChange = onMapLibreMapChange,
                onLocationManagerChange = onLocationManagerChange
            )
        }
    }
}

