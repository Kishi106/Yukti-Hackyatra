package com.example.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

enum class NotificationType {
    POTHOLE_DETECTED,
    REPORT_VERIFIED,
    REPAIR_SCHEDULED,
    REPAIR_COMPLETED,
    DUPLICATE_MERGED
}

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val description: String,
    val time: String,
    val isRead: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    val todayNotifications = remember {
        mutableStateListOf(
            NotificationItem(
                id = "1",
                type = NotificationType.POTHOLE_DETECTED,
                title = "Possible pothole detected",
                description = "We detected a sudden impact. Please confirm if it was a pothole.",
                time = "10 mins ago"
            ),
            NotificationItem(
                id = "2",
                type = NotificationType.REPAIR_COMPLETED,
                title = "Repair completed",
                description = "Your reported pothole on Beach Road has been fixed.",
                time = "2 hours ago"
            )
        )
    }

    val yesterdayNotifications = remember {
        mutableStateListOf(
            NotificationItem(
                id = "3",
                type = NotificationType.REPORT_VERIFIED,
                title = "Report verified",
                description = "Your report for MVP Colony has been verified by the authorities.",
                time = "Yesterday, 4:30 PM",
                isRead = true
            ),
            NotificationItem(
                id = "4",
                type = NotificationType.REPAIR_SCHEDULED,
                title = "Repair scheduled",
                description = "Maintenance work is scheduled for the pothole at Dwaraka Nagar.",
                time = "Yesterday, 11:15 AM",
                isRead = true
            )
        )
    }

    val olderNotifications = remember {
        mutableStateListOf(
            NotificationItem(
                id = "5",
                type = NotificationType.DUPLICATE_MERGED,
                title = "Duplicate merged",
                description = "Your report was merged with an existing one on Siripuram road.",
                time = "Oct 20",
                isRead = true
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (todayNotifications.isNotEmpty()) {
                item {
                    NotificationHeader("Today")
                }
                items(todayNotifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onDelete = { todayNotifications.remove(notification) },
                        onMarkRead = {
                            val index = todayNotifications.indexOf(notification)
                            if (index != -1) {
                                todayNotifications[index] = notification.copy(isRead = true)
                            }
                        }
                    )
                }
            }

            if (yesterdayNotifications.isNotEmpty()) {
                item {
                    NotificationHeader("Yesterday")
                }
                items(yesterdayNotifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onDelete = { yesterdayNotifications.remove(notification) },
                        onMarkRead = {
                            val index = yesterdayNotifications.indexOf(notification)
                            if (index != -1) {
                                yesterdayNotifications[index] = notification.copy(isRead = true)
                            }
                        }
                    )
                }
            }

            if (olderNotifications.isNotEmpty()) {
                item {
                    NotificationHeader("Older")
                }
                items(olderNotifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onDelete = { olderNotifications.remove(notification) },
                        onMarkRead = {
                            val index = olderNotifications.indexOf(notification)
                            if (index != -1) {
                                olderNotifications[index] = notification.copy(isRead = true)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    onDelete: () -> Unit,
    onMarkRead: () -> Unit
) {
    val (icon, color) = when (notification.type) {
        NotificationType.POTHOLE_DETECTED -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        NotificationType.REPORT_VERIFIED -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        NotificationType.REPAIR_SCHEDULED -> Icons.Default.Build to MaterialTheme.colorScheme.secondary
        NotificationType.REPAIR_COMPLETED -> Icons.Default.DoneAll to MaterialTheme.colorScheme.tertiary
        NotificationType.DUPLICATE_MERGED -> Icons.Default.CallMerge to Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) MaterialTheme.colorScheme.surface 
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 1.dp else 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = notification.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                if (!notification.isRead) {
                    TextButton(onClick = onMarkRead) {
                        Text("Mark Read")
                    }
                }
                TextButton(onClick = { /* Open Details */ }) {
                    Text("Open")
                }
            }
        }
    }
}
