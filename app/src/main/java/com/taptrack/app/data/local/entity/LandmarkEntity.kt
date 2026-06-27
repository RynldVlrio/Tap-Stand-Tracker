package com.taptrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "landmarks")
data class LandmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val color: Int = 0xFFFF9800.toInt(),
    val iconType: String = "landmark",
    val createdAt: Long = System.currentTimeMillis()
)
