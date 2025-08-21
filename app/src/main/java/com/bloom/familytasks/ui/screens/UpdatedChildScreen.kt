// app/src/main/java/com/bloom/familytasks/ui/screens/UpdatedChildScreen.kt
package com.bloom.familytasks.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloom.familytasks.data.models.ChoreAssignment
import com.bloom.familytasks.data.models.TaskStatus
import com.bloom.familytasks.data.models.MessageType
import com.bloom.familytasks.ui.components.BottomChatBar
import com.bloom.familytasks.viewmodel.EnhancedTaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatedChildScreen(
    viewModel: EnhancedTaskViewModel,
    childName: String = "Johnny",
    onNavigateHome: () -> Unit = {},
    onNavigateToChat: () -> Unit = {}
) {
    // Banner types enum - defined inside the composable

    val assignments by viewModel.choreAssignments.collectAsState()
    val myAssignments = assignments.filter { it.assignedTo == "Johnny" }
    var selectedAssignment by remember { mutableStateOf<ChoreAssignment?>(null) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    // Chat input state
    var chatMessage by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Success banner state
    var showSuccessBanner by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var bannerType by remember { mutableStateOf<BannerType>(BannerType.Submitted) }

    // Functions for banner management
    fun showSubmitted(message: String) {
        successMessage = message
        bannerType = BannerType.Submitted
        showSuccessBanner = true
    }

    fun showSuccess(message: String) {
        successMessage = message
        bannerType = BannerType.Success
        showSuccessBanner = true
    }

    fun showError(message: String) {
        successMessage = message
        bannerType = BannerType.Error
        showSuccessBanner = true
    }

    fun hideBanner() {
        showSuccessBanner = false
    }

    // Monitor API status
    val apiStatus by viewModel.apiStatus.collectAsState()
    var hasShownSuccessForCurrentStatus by remember { mutableStateOf(false) }

    LaunchedEffect(apiStatus) {
        when (apiStatus) {
            is com.bloom.familytasks.repository.ApiStatus.Loading -> {
                hasShownSuccessForCurrentStatus = false
            }
            is com.bloom.familytasks.repository.ApiStatus.Success -> {
                if (!hasShownSuccessForCurrentStatus && showSuccessBanner) {
                    showSuccess("✅ Success! Task completed successfully!")
                    hasShownSuccessForCurrentStatus = true
                }
            }
            is com.bloom.familytasks.repository.ApiStatus.Error -> {
                if (showSuccessBanner) {
                    showError("❌ Failed to submit. Please try again.")
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
            showSuccessBanner = false
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = "Points",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${myAssignments.filter { it.status == TaskStatus.VALIDATED }.sumOf { it.chore.points }}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 16.dp)
                        )
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
                if (myAssignments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { hideBanner() },
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
                            ) { hideBanner() },
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
                                        hideBanner()
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
                                            Text(
                                                "Earned $${assignment.chore.points}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
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
                        hideBanner()
                        chatMessage = it
                    },
                    selectedImages = selectedImages,
                    onImagesSelected = {
                        hideBanner()
                        selectedImages = it
                    },
                    onImagePickerClick = {
                        hideBanner()
                        imagePickerLauncher.launch("image/*")
                    },
                    onSendClick = {
                        if (chatMessage.isNotBlank()) {
                            hideBanner()
                            showSubmitted("📤 Sending message...")

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

            // Success Banner - Same as parent screen
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
                                    // import androidx.compose.ui.res.painterResource
                                    // import androidx.compose.foundation.Image
                                    // Image(
                                    //     painter = painterResource(id = R.drawable.we_bloom_logo),
                                    //     contentDescription = "WE BLOOM",
                                    //     modifier = Modifier.height(40.dp)
                                    // )

                                    // Placeholder representation
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

    // Photo submission dialog
    if (showSubmitDialog) {
        SimplePhotoSubmissionDialog(
            assignment = selectedAssignment,
            viewModel = viewModel,
            onDismiss = {
                hideBanner()
                showSubmitDialog = false
            },
            onSubmit = { images, message ->
                selectedAssignment?.let {
                    showSubmitted("📸 Submitting task with photos...")
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
                Text(
                    "$${assignment.chore.points}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
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