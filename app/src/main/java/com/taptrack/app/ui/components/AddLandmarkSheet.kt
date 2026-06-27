package com.taptrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taptrack.app.ui.screens.map.LandmarkIconDef
import com.taptrack.app.ui.screens.map.LandmarkIcons
import com.taptrack.app.utils.formatCoordinates

private val COLOR_PRESETS = listOf(
    0xFFFF9800.toInt(), 0xFF1976D2.toInt(), 0xFF01579B.toInt(), 0xFF00897B.toInt(),
    0xFFE65100.toInt(), 0xFF7B1FA2.toInt(), 0xFF388E3C.toInt(), 0xFFC62828.toInt(),
    0xFF0288D1.toInt(), 0xFF607D8B.toInt()
)

@Composable
fun AddLandmarkSheet(
    latitude: Double,
    longitude: Double,
    onSave: (name: String, description: String, iconType: String, color: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
    initialDescription: String = "",
    initialIconType: String = LandmarkIcons.LANDMARK.key,
    initialColor: Int = LandmarkIcons.LANDMARK.defaultColor,
    saveButtonLabel: String = "Save"
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var selectedIconKey by remember { mutableStateOf(initialIconType) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with live icon preview
        val currentDef = LandmarkIcons.fromKey(selectedIconKey)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(selectedColor).copy(alpha = 0.15f))
                    .border(1.5.dp, Color(selectedColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(currentDef.icon, contentDescription = null, tint = Color(selectedColor), modifier = Modifier.size(20.dp))
            }
            Column {
                Text("Pin Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = formatCoordinates(latitude, longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            placeholder = { Text("e.g. Main pump station, Gate valve 3") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Notes (optional)") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Icon type picker ─────────────────────────────────────────────────
        Text(
            "Type",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(LandmarkIcons.all) { def ->
                val isSelected = def.key == selectedIconKey
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.clickable {
                        selectedIconKey = def.key
                        selectedColor = def.defaultColor
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Color(def.defaultColor).copy(alpha = if (isSelected) 0.22f else 0.09f)
                            )
                            .then(
                                if (isSelected)
                                    Modifier.border(2.dp, Color(def.defaultColor), CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            def.icon,
                            contentDescription = def.label,
                            tint = Color(def.defaultColor),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = def.label.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color(def.defaultColor)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        // ── Color picker ─────────────────────────────────────────────────────
        Text(
            "Color",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(COLOR_PRESETS) { colorInt ->
                val isSelected = colorInt == selectedColor
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .then(
                            if (isSelected)
                                Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { selectedColor = colorInt },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ── Actions ──────────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    if (name.isNotBlank())
                        onSave(name.trim(), description.trim(), selectedIconKey, selectedColor)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text(saveButtonLabel) }
        }
    }
}
