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
}

