package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.UserPrefs
import com.example.network.ApiResult
import com.example.network.CreateUserResult
import com.example.network.UserRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userPrefs = remember { UserPrefs(context) }
    val userRepository = remember { UserRepository() }

    val cachedUser by userPrefs.cachedUserFlow.collectAsState(initial = null)

    var isEditing by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var conflictUserId by remember { mutableStateOf<String?>(null) }

    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var wardInput by remember { mutableStateOf("") }

    // Pre-fill the edit form with the current cached values whenever we enter edit mode.
    LaunchedEffect(isEditing, cachedUser) {
        val user = cachedUser
        if (isEditing && user != null) {
            nameInput = user.name
            phoneInput = user.phone
            wardInput = user.ward.orEmpty()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (cachedUser == null || isEditing) {
                    val editingExisting = isEditing && cachedUser != null

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!editingExisting) {
                            Text(
                                text = "Create your account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                        )
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                        )
                        OutlinedTextField(
                            value = wardInput,
                            onValueChange = { wardInput = it },
                            label = { Text("Ward (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) }
                        )

                        Button(
                            onClick = {
                                errorMessage = null
                                conflictUserId = null
                                coroutineScope.launch {
                                    isSubmitting = true
                                    try {
                                        if (editingExisting) {
                                            val userId = cachedUser?.id
                                            if (userId != null) {
                                                when (val result = userRepository.updateUser(
                                                    id = userId,
                                                    name = nameInput,
                                                    phone = phoneInput,
                                                    ward = wardInput.ifBlank { null }
                                                )) {
                                                    is ApiResult.Success -> {
                                                        userPrefs.save(result.data.id, result.data.name, result.data.phone, result.data.ward)
                                                        isEditing = false
                                                    }
                                                    is ApiResult.Error -> errorMessage = result.message
                                                }
                                            }
                                        } else {
                                            when (val result = userRepository.createUser(nameInput, phoneInput, wardInput.ifBlank { null })) {
                                                is CreateUserResult.Success -> {
                                                    userPrefs.save(result.user.id, result.user.name, result.user.phone, result.user.ward)
                                                }
                                                is CreateUserResult.Conflict -> {
                                                    errorMessage = result.message
                                                    conflictUserId = result.existingUserId
                                                }
                                                is CreateUserResult.Error -> errorMessage = result.message
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Something went wrong. Please try again."
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSubmitting && nameInput.isNotBlank() && phoneInput.isNotBlank()
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(if (editingExisting) "Save" else "Create Account")
                            }
                        }

                        if (editingExisting) {
                            TextButton(
                                onClick = {
                                    isEditing = false
                                    errorMessage = null
                                    conflictUserId = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel")
                            }
                        }

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (conflictUserId != null) {
                            OutlinedButton(
                                onClick = {
                                    val id = conflictUserId ?: return@OutlinedButton
                                    coroutineScope.launch {
                                        isSubmitting = true
                                        try {
                                            when (val result = userRepository.getUser(id)) {
                                                is ApiResult.Success -> {
                                                    userPrefs.save(result.data.id, result.data.name, result.data.phone, result.data.ward)
                                                    errorMessage = null
                                                    conflictUserId = null
                                                }
                                                is ApiResult.Error -> errorMessage = result.message
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Something went wrong. Please try again."
                                        } finally {
                                            isSubmitting = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Restore this account on this device")
                            }
                        }
                    }
                } else {
                    val user = cachedUser
                    if (user != null) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!user.ward.isNullOrBlank()) {
                            Text(
                                text = "Ward: ${user.ward}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "My reports",
                    value = "12",
                    icon = Icons.Default.Assessment,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate("reports") }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Verified",
                    value = "8",
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Score",
                    value = "450",
                    icon = Icons.Default.Star,
                    color = Color(0xFFFBC02D)
                )
            }

            // Actions List
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (cachedUser != null) {
                        ActionRow(icon = Icons.Default.Edit, label = "Edit Profile", onClick = { isEditing = !isEditing })
                        HorizontalDivider()
                    }
                    ActionRow(icon = Icons.Default.Settings, label = "Settings", onClick = { navController.navigate("settings") })
                    HorizontalDivider()
                    ActionRow(icon = Icons.Default.HelpOutline, label = "Help")
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            // Logout Button
            OutlinedButton(
                onClick = { /* Handle Logout */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, color: Color, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(16.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
