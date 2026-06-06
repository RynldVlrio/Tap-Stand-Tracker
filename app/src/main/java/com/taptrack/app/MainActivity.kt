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
import com.taptrack.app.ui.components.UpdateDialog
import com.taptrack.app.ui.navigation.AppNavigation
import com.taptrack.app.ui.theme.TapTrackTheme
import com.taptrack.app.utils.UpdateInfo
import com.taptrack.app.utils.checkForUpdate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TapTrackTheme {
                var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

                LaunchedEffect(Unit) {
                    updateInfo = checkForUpdate(BuildConfig.VERSION_NAME)
                }

                updateInfo?.let { info ->
                    UpdateDialog(
                        latestVersion = info.latestVersion,
                        onUpdate = {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl)))
                            updateInfo = null
                        },
                        onDismiss = { updateInfo = null }
                    )
                }

                AppNavigation()
            }
        }
    }
}
