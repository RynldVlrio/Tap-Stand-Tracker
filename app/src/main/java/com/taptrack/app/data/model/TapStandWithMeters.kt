package com.taptrack.app.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.taptrack.app.data.local.entity.TapStandEntity
import com.taptrack.app.data.local.entity.WaterMeterEntity

data class TapStandWithMeters(
    @Embedded val tapStand: TapStandEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tapStandId"
    )
    val meters: List<WaterMeterEntity>
)
