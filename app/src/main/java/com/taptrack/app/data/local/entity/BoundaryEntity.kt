package com.taptrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "boundaries")
data class BoundaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filePath: String,
    val fileType: String,
    val color: Int = 0xFF2196F3.toInt(),
    val isVisible: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
