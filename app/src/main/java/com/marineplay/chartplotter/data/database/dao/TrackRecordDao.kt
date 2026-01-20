package com.marineplay.chartplotter.data.database.dao

import androidx.room.*
import com.marineplay.chartplotter.data.database.entities.TrackRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * TrackRecord DAO
 */
@Dao
interface TrackRecordDao {
    @Query("SELECT * FROM track_records WHERE trackId = :trackId ORDER BY startTime DESC")
    fun getRecordsByTrackId(trackId: String): Flow<List<TrackRecordEntity>>
    
    @Query("SELECT * FROM track_records WHERE trackId = :trackId ORDER BY startTime DESC")
    suspend fun getRecordsByTrackIdSuspend(trackId: String): List<TrackRecordEntity>
    
    @Query("SELECT * FROM track_records WHERE id = :recordId")
    suspend fun getRecordById(recordId: String): TrackRecordEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TrackRecordEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<TrackRecordEntity>)
    
    @Delete
    suspend fun deleteRecord(record: TrackRecordEntity)
    
    @Query("DELETE FROM track_records WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: String)
    
    @Query("DELETE FROM track_records WHERE trackId = :trackId")
    suspend fun deleteRecordsByTrackId(trackId: String)
    
    /**
     * 날짜별 항적 기록 조회
     */
    @Query("SELECT * FROM track_records WHERE date = :date ORDER BY startTime DESC")
    suspend fun getRecordsByDate(date: String): List<TrackRecordEntity>
    
    /**
     * 특정 항적의 날짜별 기록 조회
     */
    @Query("SELECT * FROM track_records WHERE trackId = :trackId AND date = :date ORDER BY startTime DESC")
    suspend fun getRecordsByTrackIdAndDate(trackId: String, date: String): List<TrackRecordEntity>
    
    /**
     * 날짜 목록 조회 (중복 제거)
     */
    @Query("SELECT DISTINCT date FROM track_records ORDER BY date DESC")
    suspend fun getAllDates(): List<String>
}

