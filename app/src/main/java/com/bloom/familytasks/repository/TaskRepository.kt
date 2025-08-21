// app/src/main/java/com/bloom/familytasks/repository/TaskRepository.kt
package com.bloom.familytasks.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.material.icons.filled.Assignment
import com.bloom.familytasks.data.models.*
import com.bloom.familytasks.network.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class TaskRepository(private val context: Context) {
    private val apiService = NetworkModule.apiService
    private val gson = Gson()

    private val _taskAssignments = MutableStateFlow<List<ChoreAssignment>>(emptyList())
    val taskAssignments: StateFlow<List<ChoreAssignment>> = _taskAssignments

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _apiStatus = MutableStateFlow<ApiStatus>(ApiStatus.Idle)
    val apiStatus: StateFlow<ApiStatus> = _apiStatus

    // TEMPORARY: Simple local custom chore creation (bypasses n8n for now)
    suspend fun sendCustomChoreRequest(
        customChoreDescription: String,
        senderName: String,
        childName: String
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            _apiStatus.value = ApiStatus.Loading

            Log.d("TaskRepository", "Creating local custom chore: $customChoreDescription")

            // Create a custom chore locally (no API call)
            val customChore = Chore(
                id = System.currentTimeMillis().toInt(),
                name = extractChoreTitle(customChoreDescription),
                description = customChoreDescription,
                icon = androidx.compose.material.icons.Icons.Default.Assignment,
                points = 5, // Default points for custom chores
                category = ChoreCategory.CUSTOM,
                isCustom = true,
                createdBy = senderName
            )

            val assignment = ChoreAssignment(
                id = "custom_${System.currentTimeMillis()}",
                chore = customChore,
                assignedTo = childName,
                assignedBy = senderName,
                status = TaskStatus.PENDING,
                comments = "Custom chore created via chat"
            )

            _taskAssignments.value = _taskAssignments.value + assignment

            // Create chat messages
            val requestMessage = ChatMessage(
                sender = senderName,
                content = customChoreDescription,
                messageType = MessageType.CUSTOM_CHORE_REQUEST
            )

            val responseMessage = ChatMessage(
                sender = "Task System",
                content = "✅ Custom chore created for $childName: ${customChore.name}\n\n📝 ${customChore.description}\n\n🏆 Points: ${customChore.points}",
                messageType = MessageType.TASK_ASSIGNMENT,
                relatedTaskId = assignment.id
            )

            _chatMessages.value = _chatMessages.value + listOf(requestMessage, responseMessage)

            _apiStatus.value = ApiStatus.Success
            return@withContext Result.success(responseMessage)

        } catch (e: Exception) {
            Log.e("TaskRepository", "Error creating custom chore: ${e.message}", e)
            _apiStatus.value = ApiStatus.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    // Helper function to extract a title from the description
    private fun extractChoreTitle(description: String): String {
        return when {
            description.length <= 30 -> description
            description.contains("clean", ignoreCase = true) -> "Clean ${extractTarget(description)}"
            description.contains("organize", ignoreCase = true) -> "Organize ${extractTarget(description)}"
            description.contains("prep", ignoreCase = true) -> "Prep ${extractTarget(description)}"
            description.contains("help", ignoreCase = true) -> "Help with ${extractTarget(description)}"
            else -> description.split(" ").take(4).joinToString(" ")
        }
    }

    private fun extractTarget(description: String): String {
        val words = description.lowercase().split(" ")
        val targets = listOf("room", "kitchen", "bathroom", "bedroom", "living room", "playroom", "toys", "clothes", "dishes", "homework", "college", "school", "work")

        for (target in targets) {
            if (description.contains(target, ignoreCase = true)) {
                return target
            }
        }

        return "task"
    }

    // SIMPLIFIED: Send general chat message (local only for now)
    suspend fun sendChatMessage(
        message: String,
        senderName: String,
        messageType: MessageType = MessageType.GENERAL,
        relatedTaskId: String? = null,
        images: List<Uri> = emptyList()
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            _apiStatus.value = ApiStatus.Loading

            // Create the message locally
            val chatMessage = ChatMessage(
                sender = senderName,
                content = message,
                messageType = messageType,
                relatedTaskId = relatedTaskId,
                images = images.map { it.toString() }
            )

            _chatMessages.value = _chatMessages.value + chatMessage

            // Simple auto-response for different message types
            val autoResponse = when (messageType) {
                MessageType.HELP_REQUEST -> "I'll help you with that! What specifically do you need help with?"
                MessageType.CHORE_QUESTION -> "That's a great question! Let me help you understand."
                MessageType.CHORE_SUGGESTION -> "Thanks for the suggestion! I'll consider adding that as a chore."
                else -> null
            }

            autoResponse?.let { response ->
                val responseMessage = ChatMessage(
                    sender = "Assistant",
                    content = response,
                    messageType = MessageType.GENERAL,
                    relatedTaskId = relatedTaskId
                )
                _chatMessages.value = _chatMessages.value + responseMessage
            }

            _apiStatus.value = ApiStatus.Success
            return@withContext Result.success(chatMessage)

        } catch (e: Exception) {
            _apiStatus.value = ApiStatus.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    // Keep your existing working method for predefined chores
    suspend fun sendParentTaskRequest(
        chore: Chore,
        childName: String,
        parentId: String = "parent_admin"
    ): Result<ChoreAssignment> = withContext(Dispatchers.IO) {
        try {
            _apiStatus.value = ApiStatus.Loading

            val chatInputData = ChatInputData(
                message = "${chore.name}: ${chore.description}",
                senderId = parentId,
                childName = childName,
                taskId = "task_${System.currentTimeMillis()}_$childName"
            )

            val request = ChatRequest(
                chatInput = gson.toJson(chatInputData)
            )

            val response = apiService.sendChatMessage(
                webhookId = NetworkModule.WEBHOOK_ID,
                request = request
            )

            if (response.isSuccessful) {
                val rawResponse = response.body()?.string() ?: "No Body"
                Log.d("TaskRepository", "Raw response: $rawResponse")

                val jsonResponse = JSONObject(gson.fromJson(rawResponse, Map::class.java))
                val cleanResponse = cleanJsonString(jsonResponse.getString("output"))
                val chatResponse = gson.fromJson(cleanResponse, ChatResponse::class.java)

                val assignment = ChoreAssignment(
                    id = chatResponse.taskId.toString(),
                    chore = chore,
                    assignedTo = chatResponse.childName.toString(),
                    assignedBy = parentId,
                    status = TaskStatus.PENDING,
                    comments = chatResponse.checklist?.joinToString("\n") ?: ""
                )

                _taskAssignments.value = _taskAssignments.value + assignment

                // Add to chat
                val message = ChatMessage(
                    sender = "Task Agent",
                    content = buildString {
                        append("📋 New Task: ${chatResponse.title}\n\n")
                        append("${chatResponse.description}\n\n")
                        chatResponse.checklist?.let { checklist ->
                            append("✅ Checklist:\n")
                            checklist.forEach { item -> append("• $item\n") }
                        }
                        append("\n🏆 Points Available: ${chatResponse.pointsAvailable?.totalPossible ?: chore.points}")
                    },
                    messageType = MessageType.TASK_ASSIGNMENT,
                    relatedTaskId = assignment.id
                )
                _chatMessages.value = _chatMessages.value + message

                _apiStatus.value = ApiStatus.Success
                return@withContext Result.success(assignment)
            }

            _apiStatus.value = ApiStatus.Error("Failed to create task")
            Result.failure(Exception("Failed to create task"))
        } catch (e: Exception) {
            _apiStatus.value = ApiStatus.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    // Keep existing methods unchanged
    suspend fun submitTaskWithImages(
        assignmentId: String,
        childId: String,
        imageUris: List<Uri>,
        completionMessage: String
    ): Result<ValidationResponse> = withContext(Dispatchers.IO) {
        try {
            _apiStatus.value = ApiStatus.Loading

            val assignment = _taskAssignments.value.find { it.id == assignmentId }
                ?: return@withContext Result.failure(Exception("Assignment not found"))

            val imageAttachments = imageUris.map { uri ->
                convertUriToBase64(uri)
            }

            val chatInputData = ChatInputData(
                message = completionMessage,
                senderId = "child_$childId",
                taskId = assignmentId,
                parentTaskMessage = "${assignment.chore.name}: ${assignment.chore.description}",
                images = imageAttachments
            )

            val request = ChatRequest(
                chatInput = gson.toJson(chatInputData)
            )

            val response = apiService.sendChatMessage(
                webhookId = NetworkModule.WEBHOOK_ID,
                request = request
            )

            if (response.isSuccessful) {
                val updatedAssignment = assignment.copy(
                    status = TaskStatus.VALIDATED,
                    validationImages = imageUris.map { it.toString() }
                )

                _taskAssignments.value = _taskAssignments.value.map { a ->
                    if (a.id == assignmentId) updatedAssignment else a
                }

                val message = ChatMessage(
                    sender = "Validation Agent",
                    content = buildString {
                        append("✨ Task Validated!\n\n")
                        append("Great job completing the task!\n")
                        append("Points earned: ${assignment.chore.points}")
                    },
                    messageType = MessageType.VALIDATION_RESULT,
                    relatedTaskId = assignmentId
                )
                _chatMessages.value = _chatMessages.value + message

                _apiStatus.value = ApiStatus.Success
                return@withContext Result.success(ValidationResponse(
                    validationComplete = true,
                    childName = childId,
                    taskTitle = assignment.chore.name,
                    totalPointsEarned = assignment.chore.points.toDouble(),
                    validationStatus = "approved",
                    feedback = "Task completed successfully!",
                    achievements = listOf("Task completed"),
                    areasDoneWell = listOf("Good effort"),
                    improvementSuggestions = emptyList(),
                    completionPercentage = 100,
                    qualityRating = "good",
                    celebration = "🎉 Great job!",
                    imagesProcessed = emptyList()
                ))
            }

            _apiStatus.value = ApiStatus.Error("Validation failed")
            Result.failure(Exception("Validation failed"))
        } catch (e: Exception) {
            _apiStatus.value = ApiStatus.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    private fun cleanJsonString(jsonString: String): String {
        var cleanedJson = jsonString.trim()
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7)
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length - 3)
        }
        return cleanedJson.trim()
    }

    private suspend fun convertUriToBase64(uri: Uri): ImageAttachment = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                ImageAttachment(
                    data = "data:image/jpeg;base64,$base64String",
                    filename = "task_image_${System.currentTimeMillis()}.jpg",
                    mimetype = "image/jpeg",
                    size = byteArray.size.toLong()
                )
            } ?: ImageAttachment()
        } catch (e: Exception) {
            ImageAttachment(url = uri.toString())
        }
    }

    fun updateAssignmentLocally(assignments: List<ChoreAssignment>) {
        _taskAssignments.value = assignments
    }

    fun addChatMessage(message: ChatMessage) {
        _chatMessages.value = _chatMessages.value + message
    }

    fun getMessagesForTask(taskId: String): List<ChatMessage> {
        return _chatMessages.value.filter { it.relatedTaskId == taskId }
    }

    fun clearChatMessages() {
        _chatMessages.value = emptyList()
    }
}

sealed class ApiStatus {
    object Idle : ApiStatus()
    object Loading : ApiStatus()
    object Success : ApiStatus()
    data class Error(val message: String) : ApiStatus()
}