package com.taptrack.app.data.local.dao

import androidx.room.*
import com.taptrack.app.data.local.entity.LandmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LandmarkDao {
    @Query("SELECT * FROM landmarks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<LandmarkEntity>>

    @Query("SELECT * FROM landmarks WHERE id = :id")
    suspend fun getById(id: Long): LandmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(landmark: LandmarkEntity): Long

    @Update
    suspend fun update(landmark: LandmarkEntity)

    @Query("DELETE FROM landmarks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
