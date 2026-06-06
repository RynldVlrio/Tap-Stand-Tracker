package com.taptrack.app.data.local.dao

import androidx.room.*
import com.taptrack.app.data.local.entity.WaterMeterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterMeterDao {

    @Query("SELECT * FROM water_meters WHERE tapStandId = :tapStandId")
    fun getForTapStand(tapStandId: Long): Flow<List<WaterMeterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meters: List<WaterMeterEntity>)

    @Query("DELETE FROM water_meters WHERE tapStandId = :tapStandId")
    suspend fun deleteForTapStand(tapStandId: Long)
}
