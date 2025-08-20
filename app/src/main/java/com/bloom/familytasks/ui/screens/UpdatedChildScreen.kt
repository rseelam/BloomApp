package com.bloom.familytasks.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.bloom.familytasks.data.models.ChoreAssignment
import com.bloom.familytasks.data.models.TaskStatus
import com.bloom.familytasks.repository.ApiStatus
import com.bloom.familytasks.viewmodel.EnhancedTaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatedChildScreen(
    viewModel: EnhancedTaskViewModel,
    childName: String = "Johnny",
    onNavigateHome: () -> Unit = {}
) {
    val assignments by viewModel.choreAssignments.collectAsState()
    val myAssignments = assignments.filter { it.assignedTo == "Johnny" }
    var selectedAssignment by remember { mutableStateOf<ChoreAssignment?>(null) }
    var showSubmitDialog by remember { mutableStateOf(false) }

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
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                },
                actions = {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = "Points",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "$${myAssignments.filter { it.status == TaskStatus.VALIDATED }.sumOf { it.chore.points }}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        if (myAssignments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
                        "Ask parent to assign tasks",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pending Tasks
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
                                    }
                                    Text(
                                        "$${assignment.chore.points}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        selectedAssignment = assignment
                                        showSubmitDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Submit with Photos")
                                }
                            }
                        }
                    }
                }

                // Completed Tasks
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
    }

    // Photo submission dialog
    if (showSubmitDialog) {
        SimplePhotoSubmissionDialog(
            assignment = selectedAssignment,
            viewModel = viewModel,
            onDismiss = { showSubmitDialog = false },
            onSubmit = { images, message ->
                selectedAssignment?.let {
                    viewModel.submitTaskWithN8n(it.id, images, message)
                }
                showSubmitDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTaskCard(
    assignment: ChoreAssignment,
    onSubmit: () -> Unit,
    isLoading: Boolean
) {
    val statusColor = when (assignment.status) {
        TaskStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
        TaskStatus.SUBMITTED -> MaterialTheme.colorScheme.secondaryContainer
        TaskStatus.VALIDATED -> MaterialTheme.colorScheme.tertiaryContainer
        TaskStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusColor)
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
                Box {
                    Icon(
                        imageVector = assignment.chore.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    if (assignment.status == TaskStatus.PENDING) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd),
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ) {
                            Text("AI", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

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
                    if (assignment.status == TaskStatus.PENDING) {
                        Text(
                            text = "📸 Take photos for AI validation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                AssistChip(
                    onClick = { },
                    label = { Text("${assignment.chore.points} pts") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(assignment.status.name.replace("_", " ")) },
                    leadingIcon = {
                        when (assignment.status) {
                            TaskStatus.PENDING -> Icon(Icons.Default.Schedule, contentDescription = null)
                            TaskStatus.SUBMITTED -> Icon(Icons.Default.HourglassBottom, contentDescription = null)
                            TaskStatus.VALIDATED -> Icon(Icons.Default.Verified, contentDescription = null)
                            TaskStatus.REJECTED -> Icon(Icons.Default.Cancel, contentDescription = null)
                            else -> null
                        }
                    }
                )

                if (assignment.status == TaskStatus.PENDING) {
                    Button(
                        onClick = onSubmit,
                        enabled = !isLoading,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit to AI")
                        }
                    }
                }
            }

            if (assignment.comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = assignment.comments,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun N8nTaskSubmissionDialog(
    assignment: ChoreAssignment?,
    viewModel: EnhancedTaskViewModel,
    onDismiss: () -> Unit,
    onSubmit: (List<Uri>, String) -> Unit,
    isLoading: Boolean
) {
    val selectedImages = remember { mutableStateListOf<Uri>() }
    var completionMessage by remember { mutableStateOf("I completed the task!") }

    val multiplePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris ->
            selectedImages.clear()
            selectedImages.addAll(uris)
        }
    )

    // Option 2: Fallback for older Android versions
    val legacyPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            selectedImages.clear()
            selectedImages.addAll(uris.take(5))
        }
    )

    // Option 3: Single image picker (most compatible)
    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                if (selectedImages.size < 5) {
                    selectedImages.add(it)
                }
            }
        }
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImages.clear()
        selectedImages.addAll(uris.take(5)) // Limit to 5 images
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(Icons.Default.CameraEnhance, contentDescription = null)
        },
        title = {
            Column {
                Text("Submit for AI Validation")
                Text(
                    "Task: ${assignment?.chore?.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = completionMessage,
                    onValueChange = { completionMessage = it },
                    label = { Text("Tell AI about your work") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("📸 Add proof photos for AI to validate:")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {// Primary button - try modern picker first
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // Use modern photo picker for Android 13+
                                multiplePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                // Use legacy picker for older versions
                                legacyPicker.launch("image/*")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && selectedImages.size < 5
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Select", fontSize = 14.sp)
                    }

                    // Secondary button - add one at a time
                    OutlinedButton(
                        onClick = {
                            singlePhotoPicker.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && selectedImages.size < 5
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add One", fontSize = 14.sp)
                    }
                }

                if (selectedImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                "Selected ${selectedImages.size} photo(s) for AI analysis",
                                style = MaterialTheme.typography.labelMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(100.dp)
                            ) {
                                items(selectedImages) { uri ->
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(uri),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        if (!isLoading) {
                                            IconButton(
                                                onClick = { selectedImages.remove(uri) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                        RoundedCornerShape(12.dp)
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
                        }
                    }
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI is validating your work...")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(selectedImages, completionMessage)
                },
                enabled = selectedImages.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Submit to AI")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
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
        selectedImages.addAll(uris.take(3)) // Limit to 3 images
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