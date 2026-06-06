package com.taptrack.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.taptrack.app.data.local.dao.TapStandDao
import com.taptrack.app.data.local.dao.WaterMeterDao
import com.taptrack.app.data.local.entity.TapStandEntity
import com.taptrack.app.data.local.entity.WaterMeterEntity

@Database(
    entities = [TapStandEntity::class, WaterMeterEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tapStandDao(): TapStandDao
    abstract fun waterMeterDao(): WaterMeterDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taptrack.db"
                ).build().also { instance = it }
            }
    }
}
