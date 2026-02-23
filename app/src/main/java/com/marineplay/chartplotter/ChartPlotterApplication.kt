package com.marineplay.chartplotter

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import com.marineplay.chartplotter.data.datasources.AISDataSource
import com.marineplay.chartplotter.data.datasources.AISDataSourceImpl
import com.marineplay.chartplotter.data.repositories.AISRepositoryImpl
import com.marineplay.chartplotter.domain.repositories.AISRepository
import com.marineplay.chartplotter.service.AISForegroundService

/**
 * ChartPlotter Application
 * 앱 전체에서 AIS 데이터를 유지하기 위해 싱글톤 AIS Repository/DataSource를 관리합니다.
 * - 앱 시작 시 AIS 자동 연결
 * - 백그라운드에서도 AIS 수신 유지 (Foreground Service)
 * - 화면 전환 시에도 AIS 연결 유지
 * - 모든 화면(차트, AIS 등)에서 동일한 AIS 데이터 공유
 */
class ChartPlotterApplication : Application() {

    private var _aisDataSource: AISDataSource? = null
    private var _aisRepository: AISRepository? = null

    val aisDataSource: AISDataSource
        get() = _aisDataSource
            ?: AISDataSourceImpl(this).also {
                _aisDataSource = it
                Log.d("[ChartPlotterApplication]", "AISDataSource 싱글톤 생성")
            }

    val aisRepository: AISRepository
        get() = _aisRepository
            ?: AISRepositoryImpl(aisDataSource).also {
                _aisRepository = it
                Log.d("[ChartPlotterApplication]", "AISRepository 싱글톤 생성")
            }

    override fun onCreate() {
        super.onCreate()
        // 앱 시작 시 AIS 연결 시도 (싱글톤 초기화 및 연결)
        aisRepository.connect(38400)
        Log.d("[ChartPlotterApplication]", "앱 시작 - AIS 자동 연결 시도")

        // Foreground Service 시작: 백그라운드에서도 AIS 수신 유지
        startAISForegroundService()
    }

    /**
     * AIS Foreground Service 시작
     * 경량 서비스로 프로세스만 유지하여 성능 영향 최소화
     */
    private fun startAISForegroundService() {
        val intent = Intent(this, AISForegroundService::class.java).apply {
            action = AISForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.d("[ChartPlotterApplication]", "AIS Foreground Service 시작 - 백그라운드 수신 유지")
    }
}
