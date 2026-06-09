package com.taptrack.app.data.repository

import com.taptrack.app.data.local.dao.ProjectDao
import com.taptrack.app.data.local.dao.TapStandDao
import com.taptrack.app.data.local.dao.WaterMeterDao
import com.taptrack.app.data.local.entity.ProjectEntity
import com.taptrack.app.data.local.entity.TapStandEntity
import com.taptrack.app.data.local.entity.WaterMeterEntity
import com.taptrack.app.data.model.TapStandWithMeters
import kotlinx.coroutines.flow.Flow

class TapStandRepository(
    private val tapStandDao: TapStandDao,
    private val waterMeterDao: WaterMeterDao,
    private val projectDao: ProjectDao
) {
    fun getAllWithMeters(): Flow<List<TapStandWithMeters>> = tapStandDao.getAllWithMeters()

    suspend fun getById(id: Long): TapStandEntity? = tapStandDao.getById(id)

    suspend fun getWithMetersById(id: Long): TapStandWithMeters? = tapStandDao.getWithMetersById(id)

    fun getMetersForTapStand(tapStandId: Long): Flow<List<WaterMeterEntity>> =
        waterMeterDao.getForTapStand(tapStandId)

    suspend fun save(tapStand: TapStandEntity, meters: List<WaterMeterEntity>): Long {
        val id = tapStandDao.insert(tapStand)
        if (meters.isNotEmpty()) {
            waterMeterDao.insertAll(meters.map { it.copy(tapStandId = id) })
        }
        return id
    }

    suspend fun update(tapStand: TapStandEntity, meters: List<WaterMeterEntity>) {
        tapStandDao.update(tapStand)
        waterMeterDao.deleteForTapStand(tapStand.id)
        if (meters.isNotEmpty()) {
            waterMeterDao.insertAll(meters.map { it.copy(tapStandId = tapStand.id) })
        }
    }

    suspend fun delete(id: Long) = tapStandDao.deleteById(id)

    suspend fun getAllTapStandsOnce(): List<TapStandEntity> = tapStandDao.getAll()

    // ── Project / Folder operations ──────────────────────────────────────────

    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAll()

    suspend fun saveProject(name: String, description: String): Long =
        projectDao.insert(ProjectEntity(name = name, description = description))

    suspend fun deleteProject(id: Long) = projectDao.deleteById(id)

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getById(id)
}
