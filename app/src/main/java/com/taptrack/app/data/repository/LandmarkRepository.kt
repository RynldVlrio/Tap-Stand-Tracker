package com.taptrack.app.data.repository

import com.taptrack.app.data.local.dao.LandmarkDao
import com.taptrack.app.data.local.entity.LandmarkEntity
import kotlinx.coroutines.flow.Flow

class LandmarkRepository(private val dao: LandmarkDao) {
    fun getAll(): Flow<List<LandmarkEntity>> = dao.getAll()
    suspend fun insert(landmark: LandmarkEntity): Long = dao.insert(landmark)
    suspend fun update(landmark: LandmarkEntity) = dao.update(landmark)
    suspend fun delete(id: Long) = dao.deleteById(id)
}
