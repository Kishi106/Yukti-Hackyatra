package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.ui.auth.ResetPasswordScreen
import com.example.ui.auth.ForgotPasswordScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.SignUpScreen
import com.example.ui.dashcam.DashcamScreen
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

private data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainApp(initialRoute: String = "login", themeViewModel: ThemeViewModel? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = setOf("home", "dashcam", "profile")
    val showBottomBar = currentRoute in bottomNavRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable("login") { LoginScreen(navController) }
            composable(
                route = "reset_password",
                deepLinks = listOf(navDeepLink { uriPattern = "spotv://reset-password" })
            ) {
                ResetPasswordScreen(navController)
            }

            composable("signup") { SignUpScreen(navController) }
            composable("forgot_password") { ForgotPasswordScreen(navController) }
            composable("home") { HomeScreen(navController) }
            composable("dashcam") { DashcamScreen() }
            composable("map") { LiveMapScreen(navController) }
            composable("report") { DetectionScreen(navController) }
            composable("manual_report") { ManualReportScreen(navController) }
            composable("confirm_pothole") { ConfirmationScreen(navController) }
            composable("notifications") { NotificationsScreen(navController) }
            composable("reports") { MyReportsScreen(navController) }
            composable("profile") { ProfileScreen(navController) }
            composable("settings") { SettingsScreen(navController, themeViewModel) }
        }

        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                FloatingBottomNavigation(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    }
}

@Composable
fun FloatingBottomNavigation(
    navController: NavController,
    currentRoute: String?
) {
    val orangeSelected = Color(0xFFFF6D00)
    val softOrangeIndicator = Color(0xFFFFE0B2).copy(alpha = 0.6f)
    val grayInactive = Color(0xFF757575)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.height(64.dp)
            ) {
                val items = listOf(
                    BottomNavItem("Home", "home", Icons.Filled.Home, Icons.Outlined.Home),
                    BottomNavItem("Dashcam", "dashcam", Icons.Filled.Videocam, Icons.Outlined.Videocam),
                    BottomNavItem("Profile", "profile", Icons.Filled.Person, Icons.Outlined.Person)
                )

                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = orangeSelected,
                            selectedTextColor = orangeSelected,
                            indicatorColor = softOrangeIndicator,
                            unselectedIconColor = grayInactive,
                            unselectedTextColor = grayInactive
                        )
                    )
                }
            }
        }
    }
}
