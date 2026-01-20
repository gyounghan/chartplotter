package com.marineplay.chartplotter.data.database.dao

import androidx.room.*
import com.marineplay.chartplotter.data.database.entities.TrackSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * TrackSettings DAO
 */
@Dao
interface TrackSettingsDao {
    @Query("SELECT * FROM track_settings WHERE id = 'default'")
    fun getSettings(): Flow<TrackSettingsEntity?>
    
    @Query("SELECT * FROM track_settings WHERE id = 'default'")
    suspend fun getSettingsSuspend(): TrackSettingsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: TrackSettingsEntity)
    
    @Update
    suspend fun updateSettings(settings: TrackSettingsEntity)
}

