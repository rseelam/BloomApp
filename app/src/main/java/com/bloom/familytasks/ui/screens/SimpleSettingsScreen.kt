package com.bloom.familytasks.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bloom.familytasks.network.NetworkModule
import com.bloom.familytasks.viewmodel.EnhancedTaskViewModel
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSettingsScreen(
    viewModel: EnhancedTaskViewModel,
    onNavigateBack: () -> Unit
) {
    var productionUrl by remember { mutableStateOf(NetworkModule.getProductionUrl()) }
    var showSaveConfirmation by remember { mutableStateOf(false) }
    var showResetConfirmationDialog by remember { mutableStateOf(false) }
    var showResetSuccessMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Production Fallback URL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Used when test environment returns 404",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Production URL Input
            OutlinedTextField(
                value = productionUrl,
                onValueChange = { productionUrl = it },
                label = { Text("Production N8N URL") },
                placeholder = { Text("https://your-n8n-instance.com/") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text("Make sure to include https:// and trailing slash")
                },
                trailingIcon = {
                    if (productionUrl != NetworkModule.getProductionUrl()) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Modified",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Webhook ID (display only)
            OutlinedTextField(
                value = NetworkModule.WEBHOOK_ID,
                onValueChange = { },
                label = { Text("Webhook ID") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                supportingText = {
                    Text("Same webhook ID used for both environments")
                }
            )

            // Test Environment URL (display only)
            OutlinedTextField(
                value = "http://192.168.86.33:5678/",
                onValueChange = { },
                label = { Text("Test Environment (Primary)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                supportingText = {
                    Text("This is tried first before falling back to production")
                }
            )

            // Save Button
            Button(
                onClick = {
                    NetworkModule.setProductionUrl(productionUrl)
                    showSaveConfirmation = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = productionUrl.isNotBlank() && productionUrl != NetworkModule.getProductionUrl()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Production URL")
            }

            // Reset to Default Button
            OutlinedButton(
                onClick = {
                    productionUrl = "https://n8n.example.com/"
                    NetworkModule.setProductionUrl(productionUrl)
                    showSaveConfirmation = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Default")
            }

            // Divider moved inside the Column
            Divider()

            // Clear All Chores Button (Red/Danger zone) - moved inside Column
            OutlinedButton(
                onClick = { showResetConfirmationDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Clear All Chores & Messages",
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Danger zone warning card - moved inside Column
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Danger Zone: Clearing will permanently remove all chores and chat messages for both Parent and Johnny",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    // Save Confirmation Snackbar
    if (showSaveConfirmation) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSaveConfirmation = false
        }

        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showSaveConfirmation = false }) {
                    Text("OK")
                }
            }
        ) {
            Text("Production URL saved successfully!")
        }
    }

    // Reset Success Message
    if (showResetSuccessMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000)
            showResetSuccessMessage = false
        }

        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            action = {
                TextButton(
                    onClick = { showResetSuccessMessage = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("OK")
                }
            }
        ) {
            Text("All chores and messages have been cleared!")
        }
    }

    // Reset Confirmation Dialog
    if (showResetConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmationDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Clear All Data?",
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text("This action will permanently delete:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• All assigned chores for Johnny")
                    Text("• All existing chores from Parent")
                    Text("• All chat messages")
                    Text("• All task history and validations")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "This cannot be undone!",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllChores()
                        showResetConfirmationDialog = false
                        showResetSuccessMessage = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Clear Everything")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showResetConfirmationDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}