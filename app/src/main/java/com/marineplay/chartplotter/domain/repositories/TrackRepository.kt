package com.marineplay.chartplotter.domain.repositories

import androidx.compose.ui.graphics.Color
import com.marineplay.chartplotter.Track
import com.marineplay.chartplotter.TrackPoint
import com.marineplay.chartplotter.TrackRecord
import com.marineplay.chartplotter.TrackSettings

/**
 * Track Repository 인터페이스
 */
interface TrackRepository {
    /**
     * 모든 항적 목록 가져오기
     */
    suspend fun getAllTracks(): List<Track>
    
    /**
     * 항적 추가
     */
    suspend fun addTrack(name: String, color: Color): Track
    
    /**
     * 항적 삭제
     */
    suspend fun deleteTrack(trackId: String): Boolean
    
    /**
     * 항적 기록 추가
     */
    suspend fun addTrackRecord(
        trackId: String,
        startTime: Long,
        endTime: Long,
        points: List<TrackPoint>
    ): TrackRecord?
    
    /**
     * 항적 기록 삭제
     */
    suspend fun deleteTrackRecord(trackId: String, recordId: String): Boolean
    
    /**
     * 항적 표시/숨김 설정
     */
    suspend fun setTrackVisibility(trackId: String, isVisible: Boolean)
    
    /**
     * 항적 설정 가져오기
     */
    suspend fun getTrackSettings(): TrackSettings
    
    /**
     * 항적 설정 저장
     */
    suspend fun saveTrackSettings(settings: TrackSettings)
}

