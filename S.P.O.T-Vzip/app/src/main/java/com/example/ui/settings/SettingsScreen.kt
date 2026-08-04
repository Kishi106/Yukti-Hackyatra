package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.viewmodels.ThemeViewModel
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, themeViewModel: ThemeViewModel? = null) {
    val isSystemDark = isSystemInDarkTheme()
    val isDarkModeState by themeViewModel?.isDarkMode?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    var darkModeEnabled by remember(isDarkModeState, isSystemDark) { mutableStateOf(isDarkModeState ?: isSystemDark) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var locationPermission by remember { mutableStateOf(true) }
    var cameraPermission by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategory("General")
            SettingsSwitchRow(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                checked = darkModeEnabled,
                onCheckedChange = { 
                    darkModeEnabled = it
                    themeViewModel?.setDarkMode(it) 
                }
            )
            SettingsSwitchRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )
            SettingsActionRow(
                icon = Icons.Default.Language,
                title = "Language",
                subtitle = "English"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            SettingsCategory("Permissions")
            SettingsSwitchRow(
                icon = Icons.Default.LocationOn,
                title = "Location Permission",
                checked = locationPermission,
                onCheckedChange = { locationPermission = it }
            )
            SettingsSwitchRow(
                icon = Icons.Default.CameraAlt,
                title = "Camera Permission",
                checked = cameraPermission,
                onCheckedChange = { cameraPermission = it }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            SettingsCategory("Legal & About")
            SettingsActionRow(icon = Icons.Default.PrivacyTip, title = "Privacy Policy")
            SettingsActionRow(icon = Icons.Default.Gavel, title = "Terms of Service")
            SettingsActionRow(icon = Icons.Default.Info, title = "About S.P.O.T-V")
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
