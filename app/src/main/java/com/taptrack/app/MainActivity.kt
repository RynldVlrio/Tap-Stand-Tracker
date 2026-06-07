package com.taptrack.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.taptrack.app.ui.components.ForceUpdateDialog
import com.taptrack.app.ui.components.MaintenanceDialog
import com.taptrack.app.ui.components.UpdateAvailableDialog
import com.taptrack.app.ui.navigation.AppNavigation
import com.taptrack.app.ui.theme.TapTrackTheme
import com.taptrack.app.utils.AppUpdateState
import com.taptrack.app.utils.fetchAppControl
import com.taptrack.app.utils.resolveUpdateState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("app_control", MODE_PRIVATE)

        setContent {
            TapTrackTheme {
                var updateState by remember { mutableStateOf<AppUpdateState>(AppUpdateState.Normal) }

                LaunchedEffect(Unit) {
                    val config = fetchAppControl()
                    if (config != null) {
                        updateState = resolveUpdateState(config, BuildConfig.VERSION_CODE, prefs)
                    }
                }

                when (val state = updateState) {
                    is AppUpdateState.UpdateAvailable -> {
                        UpdateAvailableDialog(
                            onUpdate = {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.apkUrl)))
                            },
                            onDismiss = { updateState = AppUpdateState.Normal }
                        )
                        AppNavigation()
                    }
                    is AppUpdateState.ForceUpdate -> {
                        ForceUpdateDialog(
                            onUpdate = {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.apkUrl)))
                            }
                        )
                    }
                    is AppUpdateState.Maintenance -> {
                        MaintenanceDialog(message = state.message)
                    }
                    AppUpdateState.Normal -> {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
