package com.marineplay.chartplotter.presentation.modules.ais.di

import android.content.Context
import com.marineplay.chartplotter.ChartPlotterApplication
import com.marineplay.chartplotter.data.datasources.AISDataSource
import com.marineplay.chartplotter.domain.repositories.AISRepository
import com.marineplay.chartplotter.domain.usecases.*
import com.marineplay.chartplotter.presentation.modules.ais.presentation.viewmodel.AISViewModel

/**
 * AIS 모듈 의존성 주입
 * 앱 전체에서 AIS 데이터를 유지하기 위해 Application 싱글톤을 사용합니다.
 */
object AISModule {
    
    /**
     * Application에서 싱글톤 AISRepository 가져오기
     */
    fun getAISRepository(context: Context): AISRepository {
        return (context.applicationContext as ChartPlotterApplication).aisRepository
    }
    
    /**
     * AISDataSource 생성 (싱글톤 - Application에서 관리)
     */
    fun provideAISDataSource(context: Context): AISDataSource {
        return (context.applicationContext as ChartPlotterApplication).aisDataSource
    }
    
    /**
     * AISRepository 생성 (싱글톤 - Application에서 관리)
     */
    fun provideAISRepository(context: Context): AISRepository {
        return getAISRepository(context)
    }
    
    /**
     * UseCase들 생성
     */
    fun provideGetVesselsUseCase(repository: AISRepository): GetVesselsUseCase {
        return GetVesselsUseCase(repository)
    }
    
    fun provideGetEventsUseCase(repository: AISRepository): GetEventsUseCase {
        return GetEventsUseCase(repository)
    }
    
    fun provideUpdateLocationUseCase(repository: AISRepository): UpdateLocationUseCase {
        return UpdateLocationUseCase(repository)
    }
    
    fun provideConnectAISUseCase(repository: AISRepository): ConnectAISUseCase {
        return ConnectAISUseCase(repository)
    }
    
    fun provideToggleWatchlistUseCase(repository: AISRepository): ToggleWatchlistUseCase {
        return ToggleWatchlistUseCase(repository)
    }
    
    /**
     * AISViewModel 생성
     */
    fun provideAISViewModel(
        getVesselsUseCase: GetVesselsUseCase,
        getEventsUseCase: GetEventsUseCase,
        updateLocationUseCase: UpdateLocationUseCase,
        connectAISUseCase: ConnectAISUseCase,
        toggleWatchlistUseCase: ToggleWatchlistUseCase
    ): AISViewModel {
        return AISViewModel(
            getVesselsUseCase,
            getEventsUseCase,
            updateLocationUseCase,
            connectAISUseCase,
            toggleWatchlistUseCase
        )
    }
    
    /**
     * 전체 의존성 그래프 생성
     * Application 싱글톤을 사용하여 앱 전체에서 AIS 데이터 유지
     */
    fun createAISViewModel(context: Context): AISViewModel {
        val repository = provideAISRepository(context)
        val getVesselsUseCase = provideGetVesselsUseCase(repository)
        val getEventsUseCase = provideGetEventsUseCase(repository)
        val updateLocationUseCase = provideUpdateLocationUseCase(repository)
        val connectAISUseCase = provideConnectAISUseCase(repository)
        val toggleWatchlistUseCase = provideToggleWatchlistUseCase(repository)
        
        return provideAISViewModel(
            getVesselsUseCase,
            getEventsUseCase,
            updateLocationUseCase,
            connectAISUseCase,
            toggleWatchlistUseCase
        )
    }
}

