package com.marineplay.chartplotter.ui.modules.ais.di

import android.content.Context
import com.marineplay.chartplotter.data.datasources.AISDataSource
import com.marineplay.chartplotter.data.datasources.AISDataSourceImpl
import com.marineplay.chartplotter.data.repositories.AISRepositoryImpl
import com.marineplay.chartplotter.domain.repositories.AISRepository
import com.marineplay.chartplotter.domain.usecases.*
import com.marineplay.chartplotter.ui.modules.ais.presentation.viewmodel.AISViewModel

/**
 * AIS 모듈 의존성 주입
 * 실제 프로젝트에서는 Hilt, Koin 등의 DI 프레임워크 사용 권장
 */
object AISModule {
    
    /**
     * AISDataSource 생성
     */
    fun provideAISDataSource(context: Context): AISDataSource {
        return AISDataSourceImpl(context)
    }
    
    /**
     * AISRepository 생성
     */
    fun provideAISRepository(dataSource: AISDataSource): AISRepository {
        return AISRepositoryImpl(dataSource)
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
     */
    fun createAISViewModel(context: Context): AISViewModel {
        val dataSource = provideAISDataSource(context)
        val repository = provideAISRepository(dataSource)
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

