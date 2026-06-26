package com.taptrack.app

import android.app.Application
import com.taptrack.app.data.local.database.AppDatabase
import com.taptrack.app.data.repository.BoundaryRepository
import com.taptrack.app.data.repository.LandmarkRepository
import com.taptrack.app.data.repository.TapStandRepository
import com.taptrack.app.utils.MeterLookup

class TapTrackApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy {
        TapStandRepository(database.tapStandDao(), database.waterMeterDao(), database.projectDao())
    }
    val boundaryRepository by lazy { BoundaryRepository(database.boundaryDao()) }
    val landmarkRepository by lazy { LandmarkRepository(database.landmarkDao()) }

    override fun onCreate() {
        super.onCreate()
        MeterLookup.init(this)
    }
}
