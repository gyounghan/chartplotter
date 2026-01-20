package com.marineplay.chartplotter.domain.repositories

import androidx.compose.ui.graphics.Color
import com.marineplay.chartplotter.domain.entities.Track
import com.marineplay.chartplotter.domain.entities.TrackPoint
import kotlinx.coroutines.flow.Flow

/**
 * Track Repository 인터페이스
 * TrackPoint를 Track에 직접 저장 (TrackRecord 제거)
 */
interface TrackRepository {
    /**
     * 모든 항적 목록 가져오기
     */
    suspend fun getAllTracks(): List<Track>
    
    /**
     * 항적 목록 Flow (실시간 업데이트)
     */
    fun getTracksFlow(): Flow<List<Track>>
    
    /**
     * 항적 추가
     */
    suspend fun addTrack(
        name: String, 
        color: Color,
        intervalType: String = "time",
        timeInterval: Long = 5000L,
        distanceInterval: Double = 10.0
    ): Track
    
    /**
     * 항적 삭제
     */
    suspend fun deleteTrack(trackId: String): Boolean
    
    /**
     * TrackPoint 실시간 저장 (앱 종료 시에도 데이터 손실 없음)
     */
    suspend fun addTrackPoint(trackId: String, point: TrackPoint): Boolean
    
    /**
     * TrackPoint 일괄 저장
     */
    suspend fun addTrackPoints(trackId: String, points: List<TrackPoint>): Boolean
    
    /**
     * 특정 항적의 포인트 가져오기
     */
    suspend fun getTrackPoints(trackId: String): List<TrackPoint>
    
    /**
     * 특정 항적의 날짜별 포인트 가져오기
     */
    suspend fun getTrackPointsByDate(trackId: String, date: String): List<TrackPoint>
    
    /**
     * 날짜별 포인트 가져오기 (모든 항적)
     */
    suspend fun getPointsByDate(date: String): List<Pair<String, TrackPoint>>
    
    /**
     * 특정 항적의 시간 범위별 포인트 가져오기
     */
    suspend fun getTrackPointsByTimeRange(trackId: String, startTime: Long, endTime: Long): List<TrackPoint>
    
    /**
     * 특정 항적의 최근 N개 포인트 가져오기
     */
    suspend fun getRecentTrackPoints(trackId: String, limit: Int = 2000): List<TrackPoint>
    
    /**
     * 특정 항적의 포인트 삭제
     */
    suspend fun deleteTrackPoints(trackId: String): Boolean
    
    /**
     * 특정 항적의 날짜별 포인트 삭제
     */
    suspend fun deleteTrackPointsByDate(trackId: String, date: String): Boolean
    
    /**
     * 특정 항적의 시간 범위별 포인트 삭제
     */
    suspend fun deleteTrackPointsByTimeRange(trackId: String, startTime: Long, endTime: Long): Boolean
    
    /**
     * 항적 표시/숨김 설정
     */
    suspend fun setTrackVisibility(trackId: String, isVisible: Boolean)
    
    /**
     * 항적 설정 업데이트
     */
    suspend fun updateTrackSettings(
        trackId: String,
        intervalType: String? = null,
        timeInterval: Long? = null,
        distanceInterval: Double? = null
    ): Boolean
    
    /**
     * 항적 기록 상태 업데이트
     */
    suspend fun setTrackRecording(trackId: String, isRecording: Boolean): Boolean
    
    /**
     * 현재 기록 중인 항적 목록 가져오기
     */
    suspend fun getRecordingTracks(): List<Track>
    
    /**
     * 항적의 기록 상태 확인
     */
    suspend fun isTrackRecording(trackId: String): Boolean
    
    /**
     * 모든 날짜 목록 가져오기
     */
    suspend fun getAllDates(): List<String>
    
    /**
     * 특정 항적의 날짜 목록 가져오기
     */
    suspend fun getDatesByTrackId(trackId: String): List<String>
}

