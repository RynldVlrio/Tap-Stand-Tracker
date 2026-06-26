package com.taptrack.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.taptrack.app.data.local.dao.BoundaryDao
import com.taptrack.app.data.local.dao.LandmarkDao
import com.taptrack.app.data.local.dao.ProjectDao
import com.taptrack.app.data.local.dao.TapStandDao
import com.taptrack.app.data.local.dao.WaterMeterDao
import com.taptrack.app.data.local.entity.BoundaryEntity
import com.taptrack.app.data.local.entity.LandmarkEntity
import com.taptrack.app.data.local.entity.ProjectEntity
import com.taptrack.app.data.local.entity.TapStandEntity
import com.taptrack.app.data.local.entity.WaterMeterEntity

@Database(
    entities = [
        TapStandEntity::class,
        WaterMeterEntity::class,
        ProjectEntity::class,
        BoundaryEntity::class,
        LandmarkEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tapStandDao(): TapStandDao
    abstract fun waterMeterDao(): WaterMeterDao
    abstract fun projectDao(): ProjectDao
    abstract fun boundaryDao(): BoundaryDao
    abstract fun landmarkDao(): LandmarkDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add fill color (0x30 alpha of same hue as border) and label toggle
                db.execSQL("ALTER TABLE boundaries ADD COLUMN fillColor INTEGER NOT NULL DEFAULT 807507699")
                db.execSQL("ALTER TABLE boundaries ADD COLUMN showLabel INTEGER NOT NULL DEFAULT 0")
                // Derive each row's fillColor from its existing border color (apply 0x30 alpha)
                db.execSQL("UPDATE boundaries SET fillColor = ((color & 16777215) | 805306368)")

                // Landmarks table
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS landmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        color INTEGER NOT NULL DEFAULT -26624,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
