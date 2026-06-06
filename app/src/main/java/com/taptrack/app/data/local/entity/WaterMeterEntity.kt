package com.taptrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "water_meters",
    foreignKeys = [
        ForeignKey(
            entity = TapStandEntity::class,
            parentColumns = ["id"],
            childColumns = ["tapStandId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tapStandId")]
)
data class WaterMeterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tapStandId: Long,
    val serialNumber: String,
    val consumerName: String,
    val readingDate: String,
    val initialReading: Double
)
