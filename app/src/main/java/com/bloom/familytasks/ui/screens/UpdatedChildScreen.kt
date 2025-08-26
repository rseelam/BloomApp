// Complete UpdatedChildScreen.kt with dollars instead of points
package com.bloom.familytasks.ui.screens

// Android imports
import android.net.Uri

// Activity Result imports
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Compose animation imports
import androidx.compose.animation.*
import androidx.compose.animation.core.*

// Compose foundation imports
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Your app's imports
import com.bloom.familytasks.data.models.ChoreAssignment
import com.bloom.familytasks.data.models.TaskStatus
import com.bloom.familytasks.data.models.MessageType
import com.bloom.familytasks.ui.components.BottomChatBar
import com.bloom.familytasks.ui.components.SuccessBanner
import com.bloom.familytasks.ui.components.BannerStateManager
import com.bloom.familytasks.ui.components.BannerType
import com.bloom.familytasks.viewmodel.EnhancedTaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatedChildScreen(
    viewModel: EnhancedTaskViewModel,
    childName: String = "Johnny",
    onNavigateHome: () -> Unit = {},
    onNavigateToChat: () -> Unit = {}
) {
    val assignments by viewModel.choreAssignments.collectAsState()
    val myAssignments = assignments.filter { it.assignedTo == "Johnny" }
    var selectedAssignment by remember { mutableStateOf<ChoreAssignment?>(null) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    // Chat input state
    var chatMessage by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Initialize banner manager
    val bannerManager = remember { BannerStateManager() }
    val bannerState by bannerManager.bannerState

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
                    bannerManager.showSuccess("✅ Success! Task completed successfully!")
                    hasShownSuccessForCurrentStatus = true
                }
            }
            is com.bloom.familytasks.repository.ApiStatus.Error -> {
                if (bannerState.isVisible) {
                    bannerManager.showError("❌ Failed to submit. Please try again.")
                }
            }
            else -> {}
        }
    }

    // Image picker for chat
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImages = uris.take(3)
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
                title = {
                    Column {
                        Text("Johnny's Chores")
                        Text(
                            "${myAssignments.size} chore(s) assigned",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = "Money",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "${myAssignments.filter { it.status == TaskStatus.VALIDATED }.sumOf { it.chore.points }}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 16.dp),
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
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
                if (myAssignments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { bannerManager.hide() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No tasks assigned yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Ask parent for chores using the chat below!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { bannerManager.hide() },
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val pendingTasks = myAssignments.filter { it.status == TaskStatus.PENDING }
                        if (pendingTasks.isNotEmpty()) {
                            item {
                                Text(
                                    "To Do",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(pendingTasks) { assignment ->
                                TaskCard(
                                    assignment = assignment,
                                    onSubmit = {
                                        bannerManager.hide()
                                        selectedAssignment = assignment
                                        showSubmitDialog = true
                                    }
                                )
                            }
                        }

                        val completedTasks = myAssignments.filter { it.status == TaskStatus.VALIDATED }
                        if (completedTasks.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Completed",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(completedTasks) { assignment ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = assignment.chore.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Earned ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                                Icon(
                                                    Icons.Default.AttachMoney,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.tertiary
                                                )
                                                Text(
                                                    "${assignment.chore.points}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom chat bar
                BottomChatBar(
                    message = chatMessage,
                    onMessageChange = {
                        bannerManager.hide()
                        chatMessage = it
                    },
                    selectedImages = selectedImages,
                    onImagesSelected = {
                        bannerManager.hide()
                        selectedImages = it
                    },
                    onImagePickerClick = {
                        bannerManager.hide()
                        imagePickerLauncher.launch("image/*")
                    },
                    onSendClick = {
                        if (chatMessage.isNotBlank()) {
                            bannerManager.hide()
                            bannerManager.showSubmitted("📤 Sending message...")

                            val messageType = when {
                                chatMessage.contains("help", ignoreCase = true) ||
                                        chatMessage.contains("how", ignoreCase = true) -> {
                                    viewModel.sendHelpRequest(chatMessage)
                                    MessageType.HELP_REQUEST
                                }
                                chatMessage.contains("?") -> {
                                    viewModel.sendChoreQuestion(chatMessage)
                                    MessageType.CHORE_QUESTION
                                }
                                chatMessage.contains("suggest", ignoreCase = true) ||
                                        chatMessage.contains("want to", ignoreCase = true) -> {
                                    viewModel.suggestChore(chatMessage)
                                    MessageType.CHORE_SUGGESTION
                                }
                                else -> {
                                    viewModel.sendChatMessage(
                                        message = chatMessage,
                                        messageType = MessageType.GENERAL,
                                        images = selectedImages
                                    )
                                    MessageType.GENERAL
                                }
                            }

                            chatMessage = ""
                            selectedImages = emptyList()
                        }
                    },
                    placeholderText = "Ask questions, request help, or suggest chores..."
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

    // Photo submission dialog
    if (showSubmitDialog) {
        SimplePhotoSubmissionDialog(
            assignment = selectedAssignment,
            viewModel = viewModel,
            onDismiss = {
                bannerManager.hide()
                showSubmitDialog = false
            },
            onSubmit = { images, message ->
                selectedAssignment?.let {
                    bannerManager.showSubmitted("📸 Submitting task with photos...")
                    viewModel.submitTaskWithN8n(it.id, images, message)
                }
                showSubmitDialog = false
            }
        )
    }
}

@Composable
fun TaskCard(
    assignment: ChoreAssignment,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = assignment.chore.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assignment.chore.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = assignment.chore.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (assignment.chore.isCustom) {
                        Text(
                            "✨ Custom task created by ${assignment.assignedBy}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                // Show dollar amount instead of points
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${assignment.chore.points}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (assignment.comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = assignment.comments,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Submit button
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit with Photos")
            }
        }
    }
}

@Composable
fun SimplePhotoSubmissionDialog(
    assignment: ChoreAssignment?,
    viewModel: EnhancedTaskViewModel,
    onDismiss: () -> Unit,
    onSubmit: (List<Uri>, String) -> Unit
) {
    val selectedImages = remember { mutableStateListOf<Uri>() }
    var completionMessage by remember { mutableStateOf("Chore completed!") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImages.clear()
        selectedImages.addAll(uris.take(3))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Chore") },
        text = {
            Column {
                Text("Chore: ${assignment?.chore?.name}")

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = completionMessage,
                    onValueChange = { completionMessage = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Photos (${selectedImages.size}/3)")
                }

                if (selectedImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            "${selectedImages.size} photo(s) selected",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(selectedImages, completionMessage)
                },
                enabled = selectedImages.isNotEmpty()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}