// app/src/main/java/com/bloom/familytasks/ui/screens/SimpleSettingsScreen.kt
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSettingsScreen(
    onNavigateBack: () -> Unit
) {
    var productionUrl by remember { mutableStateOf(NetworkModule.getProductionUrl()) }
    var showSaveConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("N8N Settings") },
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

            Spacer(modifier = Modifier.weight(1f))

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
}