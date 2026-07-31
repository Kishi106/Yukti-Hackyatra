package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.navigation.MainApp
import com.example.ui.theme.RoadGuardAITheme
import com.example.viewmodels.ThemeViewModel

class MainActivity : ComponentActivity() {
  private val themeViewModel: ThemeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val startAction = intent.getStringExtra("action")
    val initialRoute = if (startAction == "CONFIRM_POTHOLE") "confirm_pothole" else "home"
    
    enableEdgeToEdge()
    setContent {
      val isDarkModeState by themeViewModel.isDarkMode.collectAsState()
      val isDark = isDarkModeState ?: isSystemInDarkTheme()

      RoadGuardAITheme(darkTheme = isDark) {
        MainApp(initialRoute = initialRoute, themeViewModel = themeViewModel)
      }
    }
  }
}
