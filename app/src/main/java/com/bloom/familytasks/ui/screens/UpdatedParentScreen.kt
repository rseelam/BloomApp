// Complete UpdatedParentScreen.kt with Existing Chores feature
package com.bloom.familytasks.ui.screens

// Android imports
import android.widget.Toast

// Compose animation imports
import androidx.compose.animation.*
import androidx.compose.animation.core.*

// Compose foundation imports
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Your app's imports
import com.bloom.familytasks.data.ChoreRepository
import com.bloom.familytasks.data.models.Chore
import com.bloom.familytasks.data.models.ChoreCategory
import com.bloom.familytasks.data.models.ChoreAssignment
import com.bloom.familytasks.data.models.MessageType
import com.bloom.familytasks.data.models.TaskStatus
import com.bloom.familytasks.viewmodel.EnhancedTaskViewModel
import com.bloom.familytasks.ui.components.ParentChatBar
import com.bloom.familytasks.ui.components.SuccessBanner
import com.bloom.familytasks.ui.components.BannerStateManager
import com.bloom.familytasks.ui.components.BannerType
import com.bloom.familytasks.utils.ChatUtils

// Coroutines
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Special filter type for UI
sealed class ChoreFilter {
    object ExistingChores : ChoreFilter()
    data class Category(val category: ChoreCategory) : ChoreFilter()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatedParentScreen(
    viewModel: EnhancedTaskViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToValidation: () -> Unit,
    onNavigateHome: () -> Unit = {}
) {
    // Default to "Existing Chores" filter
    var selectedFilter by remember { mutableStateOf<ChoreFilter>(ChoreFilter.ExistingChores) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedChore by remember { mutableStateOf<Chore?>(null) }
    var chatMessage by remember { mutableStateOf("") }

    // Get all assignments for existing chores view
    val allAssignments by viewModel.choreAssignments.collectAsState()

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
                    // Automatically switch to Existing Chores tab after successful submission
                    selectedFilter = ChoreFilter.ExistingChores
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
                title = {
                    val currentFilter = selectedFilter
                    Column {
                        Text("Parent Dashboard")
                        if (currentFilter is ChoreFilter.ExistingChores) {
                            Text(
                                "${allAssignments.size} active chore(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    // Filter selector with "Existing Chores" as first option
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Existing Chores filter (default)
                        item {
                            val currentFilter = selectedFilter
                            FilterChip(
                                selected = currentFilter is ChoreFilter.ExistingChores,
                                onClick = {
                                    bannerManager.hide()
                                    selectedFilter = ChoreFilter.ExistingChores
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Existing Chores")
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        // Category filters
                        items(ChoreCategory.values().toList().filter { it != ChoreCategory.CUSTOM }) { category ->
                            val currentFilter = selectedFilter
                            FilterChip(
                                selected = currentFilter is ChoreFilter.Category &&
                                        currentFilter.category == category,
                                onClick = {
                                    bannerManager.hide()
                                    selectedFilter = ChoreFilter.Category(category)
                                },
                                label = {
                                    Text(category.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            )
                        }
                    }

                    // Content based on selected filter
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentFilter = selectedFilter
                        when (currentFilter) {
                            is ChoreFilter.ExistingChores -> {
                                // Show all existing assigned chores with their status
                                if (allAssignments.isEmpty()) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    Icons.Default.Assignment,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    "No chores assigned yet",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    "Select a category above to assign chores",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Group assignments by status
                                    val pendingAssignments = allAssignments.filter { it.status == TaskStatus.PENDING }
                                    val inProgressAssignments = allAssignments.filter { it.status == TaskStatus.IN_PROGRESS }
                                    val submittedAssignments = allAssignments.filter { it.status == TaskStatus.SUBMITTED }
                                    val validatedAssignments = allAssignments.filter { it.status == TaskStatus.VALIDATED }
                                    val rejectedAssignments = allAssignments.filter { it.status == TaskStatus.REJECTED }

                                    // Show pending tasks
                                    if (pendingAssignments.isNotEmpty()) {
                                        item {
                                            Text(
                                                "⏳ Pending",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                        items(pendingAssignments) { assignment ->
                                            ExistingChoreCard(
                                                assignment = assignment,
                                                statusColor = MaterialTheme.colorScheme.primary,
                                                statusIcon = Icons.Default.Schedule
                                            )
                                        }
                                    }

                                    // Show in-progress tasks
                                    if (inProgressAssignments.isNotEmpty()) {
                                        item {
                                            Text(
                                                "🔄 In Progress",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                        items(inProgressAssignments) { assignment ->
                                            ExistingChoreCard(
                                                assignment = assignment,
                                                statusColor = Color(0xFFFFA726), // Orange
                                                statusIcon = Icons.Default.PlayArrow
                                            )
                                        }
                                    }

                                    // Show submitted tasks
                                    if (submittedAssignments.isNotEmpty()) {
                                        item {
                                            Text(
                                                "📤 Awaiting Validation",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                        items(submittedAssignments) { assignment ->
                                            ExistingChoreCard(
                                                assignment = assignment,
                                                statusColor = Color(0xFF2196F3), // Blue
                                                statusIcon = Icons.Default.HourglassTop
                                            )
                                        }
                                    }

                                    // Show validated tasks
                                    if (validatedAssignments.isNotEmpty()) {
                                        item {
                                            Text(
                                                "✅ Completed",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                        items(validatedAssignments) { assignment ->
                                            ExistingChoreCard(
                                                assignment = assignment,
                                                statusColor = Color(0xFF4CAF50), // Green
                                                statusIcon = Icons.Default.CheckCircle
                                            )
                                        }
                                    }

                                    // Show rejected tasks
                                    if (rejectedAssignments.isNotEmpty()) {
                                        item {
                                            Text(
                                                "❌ Needs Rework",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                        items(rejectedAssignments) { assignment ->
                                            ExistingChoreCard(
                                                assignment = assignment,
                                                statusColor = MaterialTheme.colorScheme.error,
                                                statusIcon = Icons.Default.Cancel
                                            )
                                        }
                                    }
                                }
                            }

                            is ChoreFilter.Category -> {
                                // Show predefined chores for selected category
                                val filteredChores = ChoreRepository.predefinedChores.filter {
                                    it.category == currentFilter.category
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
                            // Switch to Existing Chores tab after voice submission
                            selectedFilter = ChoreFilter.ExistingChores
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
                            // Switch to Existing Chores tab after custom chore submission
                            selectedFilter = ChoreFilter.ExistingChores
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Reward: ${selectedChore?.points}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedChore?.let {
                            bannerManager.showSubmitted("📋 Assigning ${it.name} to Johnny...")
                            viewModel.assignChoreWithN8n(it, "Johnny")
                            // Switch to Existing Chores tab after assignment
                            selectedFilter = ChoreFilter.ExistingChores
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

@Composable
fun ExistingChoreCard(
    assignment: ChoreAssignment,
    statusColor: Color,
    statusIcon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = statusColor.copy(alpha = 0.3f)
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
                // Status indicator icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Chore details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assignment.chore.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Assigned to: ${assignment.assignedTo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Status: ${formatStatus(assignment.status)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Points/Time info
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$${assignment.chore.points}",
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ChatUtils.formatTimestamp(assignment.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Show additional info for custom chores
            if (assignment.chore.isCustom) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Custom chore by ${assignment.assignedBy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Show completion info for validated tasks
            if (assignment.status == TaskStatus.VALIDATED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Completed successfully",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    if (assignment.validationImages.isNotEmpty()) {
                        Text(
                            "📸 ${assignment.validationImages.size} photo(s)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Show submission info for submitted tasks
            if (assignment.status == TaskStatus.SUBMITTED && assignment.validationImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "📸 ${assignment.validationImages.size} photo(s) submitted - awaiting review",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun formatStatus(status: TaskStatus): String {
    return when (status) {
        TaskStatus.PENDING -> "Pending"
        TaskStatus.IN_PROGRESS -> "In Progress"
        TaskStatus.SUBMITTED -> "Submitted for Review"
        TaskStatus.VALIDATED -> "Completed"
        TaskStatus.REJECTED -> "Needs Rework"
    }
}