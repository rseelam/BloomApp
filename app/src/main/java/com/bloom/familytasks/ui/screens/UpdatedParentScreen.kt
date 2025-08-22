// Complete UpdatedParentScreen.kt with all imports
package com.bloom.familytasks.ui.screens

// Android imports
import android.widget.Toast

// Compose animation imports
import androidx.compose.animation.*
import androidx.compose.animation.core.*

// Compose foundation imports
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

// Material Icons imports
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

// Material3 imports
import androidx.compose.material3.*

// Compose runtime imports
import androidx.compose.runtime.*

// Compose UI imports
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Your app's imports
import com.bloom.familytasks.data.ChoreRepository
import com.bloom.familytasks.data.models.Chore
import com.bloom.familytasks.data.models.ChoreCategory
import com.bloom.familytasks.data.models.MessageType
import com.bloom.familytasks.viewmodel.EnhancedTaskViewModel
import com.bloom.familytasks.ui.components.ParentChatBar
import com.bloom.familytasks.ui.components.SuccessBanner
import com.bloom.familytasks.ui.components.BannerStateManager
import com.bloom.familytasks.ui.components.BannerType

// Coroutines
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
    var selectedCategory by remember { mutableStateOf(ChoreCategory.CLEANING) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedChore by remember { mutableStateOf<Chore?>(null) }
    var chatMessage by remember { mutableStateOf("") }

    // Initialize banner manager
    val bannerManager = remember { BannerStateManager() }
    val bannerState by bannerManager.bannerState

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Monitor API status
    val apiStatus by viewModel.apiStatus.collectAsState()
    var hasShownSuccessForCurrentStatus by remember { mutableStateOf(false) }

    LaunchedEffect(apiStatus) {
        when (apiStatus) {
            is com.bloom.familytasks.repository.ApiStatus.Loading -> {
                hasShownSuccessForCurrentStatus = false
            }
            is com.bloom.familytasks.repository.ApiStatus.Success -> {
                if (!hasShownSuccessForCurrentStatus && bannerState.isVisible) {
                    bannerManager.showSuccess("✅ Success! Chore has been sent to Johnny!")
                    hasShownSuccessForCurrentStatus = true
                }
            }
            is com.bloom.familytasks.repository.ApiStatus.Error -> {
                if (bannerState.isVisible) {
                    bannerManager.showError("❌ Failed to send chore. Please try again.")
                }
            }
            else -> {}
        }
    }

    // Clean up when leaving
    DisposableEffect(Unit) {
        onDispose {
            bannerManager.hide()
            hasShownSuccessForCurrentStatus = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Dashboard") },
                navigationIcon = {
                    IconButton(onClick = {
                        bannerManager.hide()
                        onNavigateHome()
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        bannerManager.hide()
                        onNavigateToChat()
                    }) {
                        Icon(Icons.Default.Chat, contentDescription = "Full Chat")
                    }
                    IconButton(onClick = {
                        bannerManager.hide()
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
            bannerManager.hide()
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
                            bannerManager.hide()
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
                                    bannerManager.hide()
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
                                        bannerManager.hide()
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
                        bannerManager.hide()
                        chatMessage = it
                    },
                    onSendClick = {
                        bannerManager.hide()
                        if (isRecording) {
                            // Stop recording and send voice transcription
                            viewModel.stopVoiceRecording()
                            bannerManager.showSubmitted("🎤 Voice chore request submitted to Johnny...")
                        } else if (chatMessage.isNotBlank()) {
                            // Show submitted message immediately
                            bannerManager.showSubmitted("📝 Sending custom chore to Johnny...")
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
                        bannerManager.hide()
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

            // Add the reusable success banner
            SuccessBanner(
                bannerState = bannerState,
                onDismiss = { bannerManager.hide() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // Assign predefined chore dialog
    if (showAssignDialog) {
        AlertDialog(
            onDismissRequest = {
                bannerManager.hide()
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
                            bannerManager.showSubmitted("📋 Assigning ${it.name} to Johnny...")
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
                    bannerManager.hide()
                    showAssignDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}