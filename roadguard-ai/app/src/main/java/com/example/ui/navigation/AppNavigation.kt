package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.detection.ConfirmationScreen
import com.example.ui.detection.DetectionScreen
import com.example.ui.home.HomeScreen
import com.example.ui.map.LiveMapScreen
import com.example.ui.report.ManualReportScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.reports.MyReportsScreen

import com.example.ui.profile.ProfileScreen
import com.example.ui.settings.SettingsScreen
import com.example.viewmodels.ThemeViewModel

@Composable
fun MainApp(initialRoute: String = "home", themeViewModel: ThemeViewModel? = null) {
    val navController = rememberNavController()
    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(navController) }
            composable("map") { LiveMapScreen(navController) }
            composable("report") { DetectionScreen(navController) }
            composable("manual_report") { ManualReportScreen(navController) }
            composable("confirm_pothole") { ConfirmationScreen(navController) }
            composable("notifications") { NotificationsScreen(navController) }
            composable("reports") { MyReportsScreen(navController) }
            composable("profile") { ProfileScreen(navController) }
            composable("settings") { SettingsScreen(navController, themeViewModel) }
        }
    }
}

