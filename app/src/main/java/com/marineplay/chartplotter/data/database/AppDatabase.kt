package com.marineplay.chartplotter.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.marineplay.chartplotter.data.database.dao.*
import com.marineplay.chartplotter.data.database.entities.*

/**
 * Room Database
 */
@Database(
    entities = [
        TrackEntity::class,
        TrackRecordEntity::class,
        TrackPointEntity::class,
        TrackSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun trackRecordDao(): TrackRecordDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun trackSettingsDao(): TrackSettingsDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chartplotter_database"
                )
                    .fallbackToDestructiveMigration() // 개발 중이므로 데이터 손실 허용
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

