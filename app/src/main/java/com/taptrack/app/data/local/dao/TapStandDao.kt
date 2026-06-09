package com.taptrack.app.data.local.dao

import androidx.room.*
import com.taptrack.app.data.local.entity.TapStandEntity
import com.taptrack.app.data.model.TapStandWithMeters
import kotlinx.coroutines.flow.Flow

@Dao
interface TapStandDao {

    @Transaction
    @Query("SELECT * FROM tap_stands ORDER BY createdAt DESC")
    fun getAllWithMeters(): Flow<List<TapStandWithMeters>>

    @Query("SELECT * FROM tap_stands WHERE id = :id")
    suspend fun getById(id: Long): TapStandEntity?

    @Transaction
    @Query("SELECT * FROM tap_stands WHERE id = :id")
    suspend fun getWithMetersById(id: Long): TapStandWithMeters?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tapStand: TapStandEntity): Long

    @Update
    suspend fun update(tapStand: TapStandEntity)

    @Query("DELETE FROM tap_stands WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM tap_stands ORDER BY createdAt DESC")
    suspend fun getAll(): List<TapStandEntity>
}
