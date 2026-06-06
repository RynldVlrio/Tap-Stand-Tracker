package com.taptrack.app

import android.app.Application
import com.taptrack.app.data.local.database.AppDatabase
import com.taptrack.app.data.repository.TapStandRepository

class TapTrackApplication : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy {
        TapStandRepository(database.tapStandDao(), database.waterMeterDao())
    }
}
