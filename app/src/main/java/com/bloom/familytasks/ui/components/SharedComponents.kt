// app/src/main/java/com/bloom/familytasks/ui/components/SharedComponents.kt
package com.bloom.familytasks.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.bloom.familytasks.viewmodel.EnhancedTaskViewModel

@Composable
fun BottomChatBar(
    message: String,
    onMessageChange: (String) -> Unit,
    selectedImages: List<Uri>,
    onImagesSelected: (List<Uri>) -> Unit,
    onImagePickerClick: () -> Unit,
    onSendClick: () -> Unit,
    placeholderText: String = "Type a message..."
) {
    Column {
        // Selected images preview
        if (selectedImages.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                LazyRow(
                    modifier = Modifier.padding(8.dp),
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
                                    onImagesSelected(selectedImages.filter { it != uri })
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
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Chat input bar
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
                    onClick = onImagePickerClick
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Add Images")
                }

                // Message input
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(placeholderText) },
                    minLines = 1,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                FloatingActionButton(
                    onClick = onSendClick,
                    modifier = Modifier.size(48.dp),
                    containerColor = if (message.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (message.isNotBlank())
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CustomChoreDialog(
    viewModel: EnhancedTaskViewModel,
    onDismiss: () -> Unit
) {
    var choreDescription by remember { mutableStateOf("") }
    var choreTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Custom Chore with AI")
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = choreTitle,
                    onValueChange = { choreTitle = it },
                    label = { Text("Chore Title") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Organize toy room") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = choreDescription,
                    onValueChange = { choreDescription = it },
                    label = { Text("Detailed Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Describe exactly what Johnny needs to do...") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI will create step-by-step instructions!",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AttachMoney,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Reward: $2 for custom chores",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fullDescription = if (choreTitle.isNotBlank()) {
                        "$choreTitle: $choreDescription"
                    } else {
                        choreDescription
                    }

                    if (fullDescription.isNotBlank()) {
                        viewModel.sendCustomChoreRequest(fullDescription)
                        onDismiss()
                    }
                },
                enabled = choreDescription.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send to AI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}