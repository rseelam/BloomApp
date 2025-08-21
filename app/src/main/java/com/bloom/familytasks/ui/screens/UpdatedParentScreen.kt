// app/src/main/java/com/bloom/familytasks/ui/screens/UpdatedParentScreen.kt
package com.bloom.familytasks.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bloom.familytasks.data.ChoreRepository
import com.bloom.familytasks.data.models.Chore
import com.bloom.familytasks.data.models.ChoreCategory
import com.bloom.familytasks.data.models.MessageType
import com.bloom.familytasks.viewmodel.EnhancedTaskViewModel
import com.bloom.familytasks.ui.components.BottomChatBar

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

    // Chat input state
    var chatMessage by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val context = LocalContext.current

    // Image picker for chat
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImages = uris.take(3)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToChat) {
                        Icon(Icons.Default.Chat, contentDescription = "Full Chat")
                    }
                    IconButton(onClick = onNavigateToValidation) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Validations")
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
            // Main content area
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Category selector
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ChoreCategory.values().toList().filter { it != ChoreCategory.CUSTOM }) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
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

            // Bottom chat bar
            BottomChatBar(
                message = chatMessage,
                onMessageChange = { chatMessage = it },
                selectedImages = selectedImages,
                onImagesSelected = { selectedImages = it },
                onImagePickerClick = { imagePickerLauncher.launch("image/*") },
                onSendClick = {
                    if (chatMessage.isNotBlank()) {
                        // Determine if this is a custom chore request or general message
                        if (chatMessage.contains("chore", ignoreCase = true) ||
                            chatMessage.contains("task", ignoreCase = true) ||
                            chatMessage.contains("clean", ignoreCase = true) ||
                            chatMessage.contains("organize", ignoreCase = true)) {
                            // Send as custom chore request - FIXED: pass childName
                            viewModel.sendCustomChoreRequest(chatMessage, "Johnny")
                            Toast.makeText(context, "Custom chore sent to Johnny!", Toast.LENGTH_SHORT).show()
                        } else {
                            // Send as general message
                            viewModel.sendChatMessage(
                                message = chatMessage,
                                messageType = MessageType.GENERAL,
                                images = selectedImages
                            )
                        }

                        chatMessage = ""
                        selectedImages = emptyList()
                    }
                },
                placeholderText = "Send custom chore or message to Johnny..."
            )
        }
    }

    // Assign predefined chore dialog
    if (showAssignDialog) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
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
                            viewModel.assignChoreWithN8n(it, "Johnny")
                            Toast.makeText(
                                context,
                                "✓ Chore assigned to Johnny!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showAssignDialog = false
                    }
                ) {
                    Text("Assign to Johnny")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}