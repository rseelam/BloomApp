// app/src/main/java/com/bloom/familytasks/ui/screens/EnhancedChatScreen.kt
package com.bloom.familytasks.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.bloom.familytasks.data.models.ChatMessage
import com.bloom.familytasks.data.models.MessageType
import com.bloom.familytasks.viewmodel.EnhancedTaskViewModel
import kotlinx.coroutines.launch
import com.bloom.familytasks.utils.ChatUtils
import com.bloom.familytasks.ui.components.CustomChoreDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedChatScreen(
    viewModel: EnhancedTaskViewModel,
    onNavigateBack: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val apiStatus by viewModel.apiStatus.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showCustomChoreDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImages = uris.take(3)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Family Chat")
                        Text(
                            "Speaking as: $currentUser",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Custom chore button for parents
                    if (currentUser.startsWith("Parent")) {
                        IconButton(onClick = { showCustomChoreDialog = true }) {
                            Icon(Icons.Default.AddTask, contentDescription = "Custom Chore")
                        }
                    }
                    // Help button for children
                    if (currentUser.startsWith("Johnny")) {
                        IconButton(onClick = {
                            viewModel.sendHelpRequest("Can you help me with my chores?")
                        }) {
                            Icon(Icons.Default.Help, contentDescription = "Help")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Start chatting about chores!",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    if (currentUser.startsWith("Parent"))
                                        "Send custom chore requests or check in with Johnny"
                                    else
                                        "Ask questions or request help with your chores",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(messages) { message ->
                        ChatMessageItem(
                            message = message,
                            isCurrentUser = message.sender == currentUser,
                            currentUser = currentUser
                        )
                    }
                }
            }

            // Selected images preview
            if (selectedImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages) { uri ->
                        Box(
                            modifier = Modifier.size(60.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    selectedImages = selectedImages.filter { it != uri }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Message input area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Image picker button
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Add Images")
                    }

                    // Message input
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (currentUser.startsWith("Parent"))
                                    "Send custom chore or message..."
                                else
                                    "Ask a question or request help..."
                            )
                        },
                        minLines = 1,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send button
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                // Determine message type based on content and user
                                val messageType = when {
                                    currentUser.startsWith("Parent") &&
                                            (messageText.contains("chore", ignoreCase = true) ||
                                                    messageText.contains("task", ignoreCase = true)) -> {
                                        viewModel.sendCustomChoreRequest(messageText)
                                        messageText = ""
                                        selectedImages = emptyList()
                                        return@FloatingActionButton
                                    }
                                    currentUser.startsWith("Johnny") &&
                                            (messageText.contains("help", ignoreCase = true) ||
                                                    messageText.contains("how", ignoreCase = true)) -> MessageType.HELP_REQUEST
                                    currentUser.startsWith("Johnny") &&
                                            messageText.contains("?") -> MessageType.CHORE_QUESTION
                                    else -> MessageType.GENERAL
                                }

                                viewModel.sendChatMessage(
                                    message = messageText,
                                    messageType = messageType,
                                    images = selectedImages
                                )
                                messageText = ""
                                selectedImages = emptyList()
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }

    // Custom chore dialog for parents - using parent screen's implementation
    if (showCustomChoreDialog) {
        // Note: This will use the CustomChoreDialog from UpdatedParentScreen.kt
        // Make sure to import it if needed or create a shared component
        AlertDialog(
            onDismissRequest = { showCustomChoreDialog = false },
            title = { Text("Create Custom Chore") },
            text = {
                var choreDescription by remember { mutableStateOf("") }
                Column {
                    OutlinedTextField(
                        value = choreDescription,
                        onValueChange = { choreDescription = it },
                        label = { Text("Chore Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text("Describe what Johnny needs to do...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Get the choreDescription from the TextField
                        // This is a simplified version - you may want to use the more detailed one
                        showCustomChoreDialog = false
                    }
                ) {
                    Text("Send to AI")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomChoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isCurrentUser: Boolean,
    currentUser: String
) {
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = when {
        message.messageType == MessageType.CUSTOM_CHORE_REQUEST ->
            MaterialTheme.colorScheme.primaryContainer
        message.messageType == MessageType.TASK_ASSIGNMENT ->
            MaterialTheme.colorScheme.tertiaryContainer
        message.messageType == MessageType.HELP_REQUEST ->
            MaterialTheme.colorScheme.errorContainer
        message.messageType == MessageType.CHORE_QUESTION ->
            MaterialTheme.colorScheme.secondaryContainer
        message.messageType == MessageType.VALIDATION_RESULT ->
            MaterialTheme.colorScheme.primaryContainer
        isCurrentUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isCurrentUser) 12.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 12.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Message type indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (icon, typeText) = when (message.messageType) {
                        MessageType.CUSTOM_CHORE_REQUEST -> Icons.Default.AddTask to "Custom Chore"
                        MessageType.TASK_ASSIGNMENT -> Icons.Default.Assignment to "Task"
                        MessageType.HELP_REQUEST -> Icons.Default.Help to "Help"
                        MessageType.CHORE_QUESTION -> Icons.Default.QuestionMark to "Question"
                        MessageType.VALIDATION_RESULT -> Icons.Default.Verified to "Validation"
                        MessageType.CHORE_SUGGESTION -> Icons.Default.Lightbulb to "Suggestion"
                        else -> null to null
                    }

                    if (icon != null && typeText != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = typeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Images
                if (message.images.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(message.images) { imageUri ->
                            Image(
                                painter = rememberAsyncImagePainter(imageUri),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = ChatUtils.formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// CustomChoreDialog is now in SharedComponents.kt

// formatTimestamp function moved to ChatUtils.kt