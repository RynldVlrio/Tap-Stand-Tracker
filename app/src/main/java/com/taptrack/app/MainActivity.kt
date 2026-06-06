package com.taptrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.taptrack.app.ui.navigation.AppNavigation
import com.taptrack.app.ui.theme.TapTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TapTrackTheme {
                AppNavigation()
            }
        }
    }
}
