package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.AuthSessionPrefs
import com.example.ui.navigation.MainApp
import com.example.ui.theme.RoadGuardAITheme
import com.example.viewmodels.ThemeViewModel

class MainActivity : ComponentActivity() {
  private val themeViewModel: ThemeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val startAction = intent.getStringExtra("action")
    val isConfirmPotholeDeepLink = startAction == "CONFIRM_POTHOLE"

    enableEdgeToEdge()
    setContent {
      val isDarkModeState by themeViewModel.isDarkMode.collectAsState()
      val isDark = isDarkModeState ?: isSystemInDarkTheme()

      RoadGuardAITheme(darkTheme = isDark) {
        var startRoute by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
          startRoute = when {
            // The pothole-detection notification's YES action can only ever fire
            // while Driving Mode is active, which requires already being logged
            // in to reach HomeScreen's "START DRIVING" button — so this deep link
            // intentionally skips the session check.
            isConfirmPotholeDeepLink -> "confirm_pothole"
            AuthSessionPrefs(applicationContext).isLoggedIn() -> "home"
            else -> "login"
          }
        }

        val resolvedRoute = startRoute
        if (resolvedRoute != null) {
          MainApp(initialRoute = resolvedRoute, themeViewModel = themeViewModel)
        } else {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {}
        }
      }
    }
  }
}
