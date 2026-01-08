package com.marineplay.chartplotter.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import com.marineplay.chartplotter.helpers.PointHelper
import com.marineplay.chartplotter.viewmodel.MainViewModel
import com.marineplay.chartplotter.TrackManager
import com.marineplay.chartplotter.LocationManager
import com.marineplay.chartplotter.EntryMode
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
 * - Extra Type: String (CHART_ONLY, BLACKBOX_ONLY, SPLIT)
 */
@Composable
fun ChartPlotterApp(
    entryMode: EntryMode,
    viewModel: MainViewModel,
    activity: ComponentActivity,
    pointHelper: PointHelper,
    trackManager: TrackManager,
    onMapLibreMapChange: (MapLibreMap?) -> Unit = {},
    onLocationManagerChange: (LocationManager?) -> Unit = {}
) {
    when (entryMode) {
        EntryMode.CHART_ONLY -> {
            // 차트 전용 화면
            ChartOnlyScreen(
                viewModel = viewModel,
                activity = activity,
                pointHelper = pointHelper,
                trackManager = trackManager,
                onMapLibreMapChange = onMapLibreMapChange,
                onLocationManagerChange = onLocationManagerChange
            )
        }
        EntryMode.BLACKBOX_ONLY -> {
            // 블랙박스 전용 화면 (향후 구현)
            // TODO: BlackboxOnlyScreen 구현
            ChartOnlyScreen(
                viewModel = viewModel,
                activity = activity,
                pointHelper = pointHelper,
                trackManager = trackManager,
                onMapLibreMapChange = onMapLibreMapChange,
                onLocationManagerChange = onLocationManagerChange
            )
        }
        EntryMode.SPLIT -> {
            // 화면 분할 (차트 + 블랙박스) (향후 구현)
            // TODO: SplitScreen 구현
            ChartOnlyScreen(
                viewModel = viewModel,
                activity = activity,
                pointHelper = pointHelper,
                trackManager = trackManager,
                onMapLibreMapChange = onMapLibreMapChange,
                onLocationManagerChange = onLocationManagerChange
            )
        }
    }
}

