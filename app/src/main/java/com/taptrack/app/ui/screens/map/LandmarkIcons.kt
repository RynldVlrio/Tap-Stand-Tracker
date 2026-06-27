package com.taptrack.app.ui.screens.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

data class LandmarkIconDef(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val defaultColor: Int
)

object LandmarkIcons {
    val LANDMARK        = LandmarkIconDef("landmark",         "Landmark",            Icons.Default.Star,      0xFFFF9800.toInt())
    val TAP_STAND       = LandmarkIconDef("tap_stand",        "Tap Stand",           Icons.Default.WaterDrop, 0xFF1976D2.toInt())
    val OFFICE          = LandmarkIconDef("office",           "Office",              Icons.Default.Business,  0xFF607D8B.toInt())
    val RESERVOIR       = LandmarkIconDef("reservoir",        "Reservoir",           Icons.Default.Water,     0xFF01579B.toInt())
    val TREATMENT_PLANT = LandmarkIconDef("treatment_plant",  "Treatment Plant",     Icons.Default.FilterAlt, 0xFF00897B.toInt())
    val PUMP_STATION    = LandmarkIconDef("pump_station",     "Pump Station",        Icons.Default.Bolt,      0xFFE65100.toInt())
    val DOSING_STATION  = LandmarkIconDef("dosing_station",   "Dosing Station",      Icons.Default.Science,   0xFF7B1FA2.toInt())
    val GATE_VALVE      = LandmarkIconDef("gate_valve",       "Gate Valve",          Icons.Default.Tune,      0xFF388E3C.toInt())
    val PRESSURE_VALVE  = LandmarkIconDef("pressure_valve",   "Pressure Valve",      Icons.Default.Speed,     0xFFC62828.toInt())
    val AIR_VALVE       = LandmarkIconDef("air_valve",        "Air Release Valve",   Icons.Default.Air,       0xFF0288D1.toInt())

    val all = listOf(
        LANDMARK, TAP_STAND, OFFICE, RESERVOIR, TREATMENT_PLANT,
        PUMP_STATION, DOSING_STATION, GATE_VALVE, PRESSURE_VALVE, AIR_VALVE
    )

    fun fromKey(key: String): LandmarkIconDef = all.find { it.key == key } ?: LANDMARK
}
