package com.taptrack.app.ui.screens.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Water
import androidx.compose.ui.graphics.vector.ImageVector

data class LandmarkIconDef(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val defaultColor: Int
)

object LandmarkIcons {
    val LANDMARK         = LandmarkIconDef("landmark",        "Landmark",            Icons.Default.Star,      0xFFFF9800.toInt())
    val OFFICE           = LandmarkIconDef("office",          "Office",              Icons.Default.Business,  0xFF607D8B.toInt())
    val RESERVOIR        = LandmarkIconDef("reservoir",       "Reservoir",           Icons.Default.Water,     0xFF01579B.toInt())
    val FILTRATION       = LandmarkIconDef("filtration",      "WTP",                 Icons.Default.AccountBalance, 0xFF00897B.toInt())
    val PUMP             = LandmarkIconDef("pump",            "Pump",                Icons.Default.Loop,           0xFFE65100.toInt())
    val DOSING_STATION   = LandmarkIconDef("dosing_station",  "Dosing Station",      Icons.Default.Science,   0xFF7B1FA2.toInt())
    val VALVE            = LandmarkIconDef("valve",           "Valve",               Icons.Default.Tune,      0xFF388E3C.toInt())
    val GAUGE            = LandmarkIconDef("gauge",           "Gauge",               Icons.Default.Speed,     0xFFC62828.toInt())

    val all = listOf(LANDMARK, OFFICE, RESERVOIR, FILTRATION, PUMP, DOSING_STATION, VALVE, GAUGE)

    fun fromKey(key: String): LandmarkIconDef = all.find { it.key == key } ?: LANDMARK
}
