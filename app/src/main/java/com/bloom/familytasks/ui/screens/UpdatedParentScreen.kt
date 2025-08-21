// app/src/main/java/com/bloom/familytasks/ui/screens/UpdatedParentScreen.kt
package com.bloom.familytasks.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bloom.familytasks.data.ChoreRepository
import com.bloom.familytasks.data.models.Chore
import com.bloom.familytasks.data.models.ChoreCategory
import com.bloom.familytasks.data.models.MessageType
import com.bloom.familytasks.viewmodel.EnhancedTaskViewModel
import com.bloom.familytasks.ui.components.ParentChatBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatedParentScreen(
    viewModel: EnhancedTaskViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToValidation: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    // Banner types enum - defined inside the composable

    var selectedCategory by remember { mutableStateOf(ChoreCategory.CLEANING) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedChore by remember { mutableStateOf<Chore?>(null) }

    // Chat input state
    var chatMessage by remember { mutableStateOf("") }

    // Success banner state
    var showSuccessBanner by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var bannerType by remember { mutableStateOf<BannerType>(BannerType.Submitted) }
    var lastSuccessTime by remember { mutableStateOf(0L) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Function to show submitted banner
    fun showSubmitted(message: String) {
        successMessage = message
        bannerType = BannerType.Submitted
        showSuccessBanner = true
        lastSuccessTime = System.currentTimeMillis()
    }

    // Function to show success banner
    fun showSuccess(message: String) {
        successMessage = message
        bannerType = BannerType.Success
        showSuccessBanner = true
        lastSuccessTime = System.currentTimeMillis()
    }

    // Function to show error banner
    fun showError(message: String) {
        successMessage = message
        bannerType = BannerType.Error
        showSuccessBanner = true
    }

    // Function to hide banner on any interaction
    fun hideBanner() {
        showSuccessBanner = false
    }

    // Monitor API status for success
    val apiStatus by viewModel.apiStatus.collectAsState()
    var hasShownSuccessForCurrentStatus by remember { mutableStateOf(false) }

    LaunchedEffect(apiStatus) {
        when (apiStatus) {
            is com.bloom.familytasks.repository.ApiStatus.Loading -> {
                // Don't show submitted here as we'll show it immediately on action
                hasShownSuccessForCurrentStatus = false
            }
            is com.bloom.familytasks.repository.ApiStatus.Success -> {
                // Only show success if we haven't shown it for this status yet
                if (!hasShownSuccessForCurrentStatus && showSuccessBanner) {
                    // Update the existing banner to success
                    showSuccess("✅ Success! Chore has been sent to Johnny!")
                    hasShownSuccessForCurrentStatus = true
                }
            }
            is com.bloom.familytasks.repository.ApiStatus.Error -> {
                if (showSuccessBanner) {
                    showError("❌ Failed to send chore. Please try again.")
                }
            }
            else -> {
                // For Idle state, don't show banner
            }
        }
    }

    // Clean up when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            showSuccessBanner = false
            hasShownSuccessForCurrentStatus = false
            // Reset API status when leaving the screen
            // Note: Add resetApiStatus() method to your ViewModel
            // viewModel.resetApiStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Dashboard") },
                navigationIcon = {
                    IconButton(onClick = {
                        hideBanner()
                        onNavigateHome()
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        hideBanner()
                        onNavigateToChat()
                    }) {
                        Icon(Icons.Default.Chat, contentDescription = "Full Chat")
                    }
                    IconButton(onClick = {
                        hideBanner()
                        onNavigateToValidation()
                    }) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Validations")
                    }
                }
            )
        },
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            hideBanner()
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Main content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            hideBanner()
                        }
                ) {
                    // Category selector
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ChoreCategory.values().toList().filter { it != ChoreCategory.CUSTOM }) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    hideBanner()
                                    selectedCategory = category
                                },
                                label = {
                                    Text(category.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            )
                        }
                    }

                    // Chores list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filteredChores = ChoreRepository.predefinedChores.filter {
                            it.category == selectedCategory
                        }

                        if (filteredChores.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "No tasks in this category",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            items(filteredChores) { chore ->
                                Card(
                                    onClick = {
                                        hideBanner()
                                        selectedChore = chore
                                        showAssignDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = chore.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = chore.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = chore.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = "$${chore.points}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Get voice recording state
                val isRecording by viewModel.isRecording.collectAsState()

                // Parent chat bar without image picker
                ParentChatBar(
                    message = chatMessage,
                    onMessageChange = {
                        hideBanner()
                        chatMessage = it
                    },
                    onSendClick = {
                        hideBanner()
                        if (isRecording) {
                            // Stop recording and send voice transcription
                            viewModel.stopVoiceRecording()
                            showSubmitted("🎤 Voice chore request submitted to Johnny...")
                        } else if (chatMessage.isNotBlank()) {
                            // Show submitted message immediately
                            showSubmitted("📝 Sending custom chore to Johnny...")
                            // Use unified assignChore method
                            viewModel.assignChore(
                                chore = null,
                                customDescription = chatMessage,
                                childName = "Johnny",
                                isVoiceInput = false
                            )
                            chatMessage = ""
                        }
                    },
                    onVoiceClick = {
                        hideBanner()
                        // Toggle voice recording
                        if (isRecording) {
                            viewModel.stopVoiceRecording()
                        } else {
                            viewModel.startVoiceRecording()
                            Toast.makeText(context, "Voice recording started", Toast.LENGTH_SHORT).show()
                        }
                    },
                    placeholderText = "Send custom chore or message to Johnny...",
                    isRecording = isRecording
                )
            }

            // Success Banner - Overlay at the top
            AnimatedVisibility(
                visible = showSuccessBanner,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                val bannerColor = when (bannerType) {
                    BannerType.Submitted -> MaterialTheme.colorScheme.secondaryContainer
                    BannerType.Success -> MaterialTheme.colorScheme.primaryContainer
                    BannerType.Error -> MaterialTheme.colorScheme.errorContainer
                }

                val bannerIcon = when (bannerType) {
                    BannerType.Submitted -> Icons.Default.Send
                    BannerType.Success -> Icons.Default.CheckCircle
                    BannerType.Error -> Icons.Default.Error
                }

                val bannerTitle = when (bannerType) {
                    BannerType.Submitted -> "Submitted"
                    BannerType.Success -> "Success!"
                    BannerType.Error -> "Error"
                }

                val iconTint = when (bannerType) {
                    BannerType.Submitted -> MaterialTheme.colorScheme.secondary
                    BannerType.Success -> MaterialTheme.colorScheme.primary
                    BannerType.Error -> MaterialTheme.colorScheme.error
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { hideBanner() },
                    colors = CardDefaults.cardColors(
                        containerColor = bannerColor
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Animated icon
                        if (bannerType == BannerType.Submitted) {
                            // Loading animation for submitted state
                            val infiniteTransition = rememberInfiniteTransition(label = "loading")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotation"
                            )

                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .graphicsLayer(
                                        rotationZ = rotation
                                    ),
                                tint = iconTint
                            )
                        } else if (bannerType == BannerType.Success) {
                            // Pulsing animation for success
                            val infiniteTransition = rememberInfiniteTransition(label = "success")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )

                            Icon(
                                bannerIcon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale
                                    ),
                                tint = iconTint
                            )
                        } else {
                            // Static icon for error
                            Icon(
                                bannerIcon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = iconTint
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                bannerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (bannerType) {
                                    BannerType.Submitted -> MaterialTheme.colorScheme.onSecondaryContainer
                                    BannerType.Success -> MaterialTheme.colorScheme.onPrimaryContainer
                                    BannerType.Error -> MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                            Text(
                                successMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when (bannerType) {
                                    BannerType.Submitted -> MaterialTheme.colorScheme.onSecondaryContainer
                                    BannerType.Success -> MaterialTheme.colorScheme.onPrimaryContainer
                                    BannerType.Error -> MaterialTheme.colorScheme.onErrorContainer
                                }
                            )

                            // Show WE BLOOM logo on success
                            if (bannerType == BannerType.Success) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Note: Replace with your actual image resource
                                    // For drawable resource:
                                    // Image(
                                    //     painter = painterResource(id = R.drawable.bloom_logo),
                                    //     contentDescription = "WE BLOOM",
                                    //     modifier = Modifier.height(40.dp)
                                    // )

                                    // For now, showing a placeholder representation
                                    Card(
                                        modifier = Modifier.height(32.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFFA726).copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "WE",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color(0xFFFFA726),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.LocalFlorist,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = Color(0xFF66BB6A)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "BLOOM",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color(0xFFFFA726),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = { hideBanner() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = when (bannerType) {
                                    BannerType.Submitted -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                    BannerType.Success -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    BannerType.Error -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Assign predefined chore dialog
    if (showAssignDialog) {
        AlertDialog(
            onDismissRequest = {
                hideBanner()
                showAssignDialog = false
            },
            title = { Text("Assign Chore") },
            text = {
                Column {
                    Text("Assign '${selectedChore?.name}' to Johnny?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Reward: $${selectedChore?.points}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedChore?.let {
                            showSubmitted("📋 Assigning ${it.name} to Johnny...")
                            viewModel.assignChoreWithN8n(it, "Johnny")
                        }
                        showAssignDialog = false
                    }
                ) {
                    Text("Assign to Johnny")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    hideBanner()
                    showAssignDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}