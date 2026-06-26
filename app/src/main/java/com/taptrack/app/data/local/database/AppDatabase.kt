package com.taptrack.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.taptrack.app.data.local.dao.BoundaryDao
import com.taptrack.app.data.local.dao.ProjectDao
import com.taptrack.app.data.local.dao.TapStandDao
import com.taptrack.app.data.local.dao.WaterMeterDao
import com.taptrack.app.data.local.entity.BoundaryEntity
import com.taptrack.app.data.local.entity.ProjectEntity
import com.taptrack.app.data.local.entity.TapStandEntity
import com.taptrack.app.data.local.entity.WaterMeterEntity

@Database(
    entities = [TapStandEntity::class, WaterMeterEntity::class, ProjectEntity::class, BoundaryEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tapStandDao(): TapStandDao
    abstract fun waterMeterDao(): WaterMeterDao
    abstract fun projectDao(): ProjectDao
    abstract fun boundaryDao(): BoundaryDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tap_stands ADD COLUMN folderId INTEGER")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS projects (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS boundaries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        fileType TEXT NOT NULL,
                        color INTEGER NOT NULL DEFAULT -14575885,
                        isVisible INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )"""
                )
            }
        }

        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taptrack.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
