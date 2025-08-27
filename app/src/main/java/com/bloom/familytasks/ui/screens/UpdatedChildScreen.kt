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
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.unit.sp
import com.bloom.familytasks.BuildConfig

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
                                        containerColor = Color(0xFFE8F5E9)
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
                                            tint = Color(0xFF4CAF50)
//                                            tint = MaterialTheme.colorScheme.tertiary
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
                                                    color = Color(0xFF2E7D32)
//                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                                Icon(
                                                    Icons.Default.AttachMoney,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color(0xFF2E7D32)
//                                                            tint = MaterialTheme.colorScheme.tertiary
                                                )
                                                Text(
                                                    "${assignment.chore.points}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF2E7D32),
//                                                    color = MaterialTheme.colorScheme.tertiary,
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

// Replace the TaskCard composable in UpdatedChildScreen.kt with this enhanced version

@Composable
fun TaskCard(
    assignment: ChoreAssignment,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFC773) // Orange color matching your screenshot
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with icon, title, and reward
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = assignment.chore.icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF6B4700)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assignment.chore.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E2723)
                    )
                    Text(
                        text = assignment.chore.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5D4037)
                    )
                }
                Text(
                    text = "$ ${assignment.chore.points}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Steps to Complete section - white background
            if (assignment.comments.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Steps to Complete:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Parse and display comments
                        val lines = assignment.comments.split("\n").filter { it.isNotBlank() }
                        var isInSafetySection = false
                        var isInPhotoSection = false
                        var isInBonusSection = false

                        lines.forEach { line ->
                            val cleanLine = line.trim()

                            when {
                                // Safety section header
                                cleanLine.contains("⚠️ Safety:") -> {
                                    isInSafetySection = true
                                    isInPhotoSection = false
                                    isInBonusSection = false

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color(0xFFFF6B35)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Safety Reminders",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF6B35)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                // Photo section header
                                cleanLine.contains("📸 Photos Needed:") -> {
                                    isInPhotoSection = true
                                    isInSafetySection = false
                                    isInBonusSection = false

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color(0xFF4CAF50)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Photos Required",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                // Bonus section header
                                cleanLine.contains("💰 Bonus Tips:") -> {
                                    isInBonusSection = true
                                    isInSafetySection = false
                                    isInPhotoSection = false

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color(0xFFFFC107)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Bonus Tips",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFC107)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                // Encouragement message
                                cleanLine.contains("💪") -> {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFE8F5E9)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = cleanLine.replace("💪", "").trim(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                // Bullet point items
                                cleanLine.startsWith("•") -> {
                                    val itemText = cleanLine.substring(1).trim()

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Different bullet styles for different sections
                                        val bulletIcon = when {
                                            isInSafetySection -> Icons.Default.Warning
                                            isInPhotoSection -> Icons.Default.PhotoCamera
                                            isInBonusSection -> Icons.Default.Star
                                            else -> Icons.Default.CheckCircleOutline
                                        }

                                        val bulletColor = when {
                                            isInSafetySection -> Color(0xFFFF6B35)
                                            isInPhotoSection -> Color(0xFF4CAF50)
                                            isInBonusSection -> Color(0xFFFFC107)
                                            else -> Color(0xFF757575)
                                        }

                                        Icon(
                                            imageVector = bulletIcon,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(top = 2.dp),
                                            tint = bulletColor
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = itemText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF424242),
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Fallback if no comments
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Steps to Complete:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "• Complete the task as described above\n• Make sure to do a thorough job\n• Take before and after photos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF424242)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Photo reminder section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFE0B2)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF6B4700)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Remember to take photos!",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3E2723)
                        )
                        Text(
                            "Show before & after to earn your reward",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Submit button
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B4700)
                )
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Submit with Photos",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
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