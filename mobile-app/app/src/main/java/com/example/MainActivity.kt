package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.navigation.MainApp
import com.example.ui.theme.SPOTVTheme
import com.example.viewmodels.ThemeViewModel
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
  private val themeViewModel: ThemeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    val startAction = intent.getStringExtra("action")
    supabase.handleDeeplinks(intent)

    enableEdgeToEdge()
    setContent {
      val isDarkModeState by themeViewModel.isDarkMode.collectAsState()
      val isDark = isDarkModeState ?: false
      
      var initialRoute by remember { mutableStateOf<String?>(null) }
      
      LaunchedEffect(Unit) {
          val dataString = intent.dataString
          if (dataString != null && dataString.startsWith("spotv://reset-password")) {
              initialRoute = "reset_password"
          } else if (startAction == "CONFIRM_POTHOLE") {
              initialRoute = "confirm_pothole"
          } else {
              val hasSession = com.example.repository.AuthRepository().checkSession()
              initialRoute = if (hasSession) "home" else "login"
          }
      }

      SPOTVTheme(darkTheme = isDark) {
        if (initialRoute == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            MainApp(initialRoute = initialRoute!!, themeViewModel = themeViewModel)
        }
      }
    }
  }
}
