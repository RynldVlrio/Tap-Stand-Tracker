package com.taptrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tap_stands")
data class TapStandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val locationDescription: String,
    val latitude: Double,
    val longitude: Double,
    val photoPath: String,
    val installationDate: String,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)
