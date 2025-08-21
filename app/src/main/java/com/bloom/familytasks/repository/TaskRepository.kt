// app/src/main/java/com/bloom/familytasks/repository/TaskRepository.kt
package com.bloom.familytasks.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.material.icons.Icons
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

    // ========== UNIFIED REUSABLE METHOD FOR ALL CHORE TYPES ==========
    suspend fun sendChoreToN8n(
        chore: Chore? = null,  // For predefined chores
        customDescription: String? = null,  // For custom chores
        senderName: String,
        childName: String,
        isVoiceInput: Boolean = false
    ): Result<ChoreAssignment> = withContext(Dispatchers.IO) {
        try {
            _apiStatus.value = ApiStatus.Loading

            // Build unified message for n8n
            val choreMessage = when {
                chore != null -> {
                    // Predefined chore
                    "${chore.name}: ${chore.description}"
                }
                customDescription != null -> {
                    // Custom chore (with special marker for n8n to recognize)
                    "CUSTOM_CHORE: $customDescription"
                }
                else -> {
                    throw IllegalArgumentException("Either chore or customDescription must be provided")
                }
            }

            // Create unified request structure
            val chatInputData = ChatInputData(
                message = choreMessage,
                senderId = "parent_${senderName.lowercase()}",
                childName = childName,
                taskId = "task_${System.currentTimeMillis()}_$childName",
                parentTaskMessage = if (isVoiceInput) {
                    "Voice Input: $choreMessage"
                } else {
                    choreMessage
                }
            )

            val request = ChatRequest(
                chatInput = gson.toJson(chatInputData)
            )

            Log.d("TaskRepository", "Sending to n8n: ${request.chatInput}")

            // Single API call for both predefined and custom chores
            val response = apiService.sendChatMessage(
                webhookId = NetworkModule.WEBHOOK_ID,
                request = request
            )

            if (response.isSuccessful) {
                val rawResponse = response.body()?.string() ?: "{}"
                Log.d("TaskRepository", "n8n response: $rawResponse")

                val chatResponse = parseN8nResponse(rawResponse)

                // Create ChoreAssignment from response
                val assignment = createChoreAssignment(
                    chatResponse = chatResponse,
                    originalChore = chore,
                    customDescription = customDescription,
                    senderName = senderName,
                    childName = childName,
                    isVoiceInput = isVoiceInput
                )

                // Update state
                _taskAssignments.value = _taskAssignments.value + assignment

                // Add chat messages
                addChoreMessages(assignment, chatResponse, isVoiceInput)

                _apiStatus.value = ApiStatus.Success
                return@withContext Result.success(assignment)

            } else {
                Log.e("TaskRepository", "n8n API failed: ${response.code()}")
                _apiStatus.value = ApiStatus.Error("API call failed: ${response.code()}")

                // Fallback to local creation
                return@withContext createLocalChoreAssignment(
                    chore = chore,
                    customDescription = customDescription,
                    senderName = senderName,
                    childName = childName,
                    isVoiceInput = isVoiceInput
                )
            }

        } catch (e: Exception) {
            Log.e("TaskRepository", "Error calling n8n API: ${e.message}", e)
            _apiStatus.value = ApiStatus.Error(e.message ?: "Unknown error")

            // Fallback to local creation
            return@withContext createLocalChoreAssignment(
                chore = chore,
                customDescription = customDescription,
                senderName = senderName,
                childName = childName,
                isVoiceInput = isVoiceInput
            )
        }
    }

    // ========== HELPER METHODS (REUSABLE) ==========

    private fun parseN8nResponse(rawResponse: String): ChatResponse {
        return try {
            val jsonResponse = JSONObject(gson.fromJson(rawResponse, Map::class.java))
            val cleanResponse = cleanJsonString(jsonResponse.optString("output", "{}"))
            gson.fromJson(cleanResponse, ChatResponse::class.java)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error parsing n8n response", e)
            // Return a default response
            ChatResponse(
                taskId = "task_${System.currentTimeMillis()}",
                title = "Task Created",
                description = "Task has been assigned"
            )
        }
    }

    private fun createChoreAssignment(
        chatResponse: ChatResponse,
        originalChore: Chore?,
        customDescription: String?,
        senderName: String,
        childName: String,
        isVoiceInput: Boolean
    ): ChoreAssignment {
        val chore = originalChore ?: Chore(
            id = System.currentTimeMillis().toInt(),
            name = chatResponse.title ?: extractChoreTitle(customDescription ?: "Custom Task"),
            description = chatResponse.description ?: customDescription ?: "Custom task",
            icon = Icons.Default.Assignment,
            points = chatResponse.pointsAvailable?.totalPossible ?: 5,
            category = ChoreCategory.CUSTOM,
            isCustom = true,
            createdBy = senderName
        )

        return ChoreAssignment(
            id = chatResponse.taskId ?: "task_${System.currentTimeMillis()}",
            chore = chore,
            assignedTo = childName,
            assignedBy = senderName,
            status = TaskStatus.PENDING,
            comments = buildChoreComments(chatResponse, isVoiceInput)
        )
    }

    private fun buildChoreComments(chatResponse: ChatResponse, isVoiceInput: Boolean): String {
        return buildString {
            chatResponse.checklist?.forEach { item ->
                append("• $item\n")
            }
            if (chatResponse.safetyReminders?.isNotEmpty() == true) {
                append("\n⚠️ Safety:\n")
                chatResponse.safetyReminders.forEach { reminder ->
                    append("• $reminder\n")
                }
            }
            if (isVoiceInput) {
                append("\n🎤 Created via voice command")
            }
        }
    }

    private fun addChoreMessages(
        assignment: ChoreAssignment,
        chatResponse: ChatResponse,
        isVoiceInput: Boolean
    ) {
        val requestMessage = ChatMessage(
            sender = assignment.assignedBy,
            content = if (assignment.chore.isCustom) {
                if (isVoiceInput) "🎤 Voice: ${assignment.chore.description}"
                else "Custom chore: ${assignment.chore.description}"
            } else {
                "Assigned: ${assignment.chore.name}"
            },
            messageType = if (assignment.chore.isCustom) MessageType.CUSTOM_CHORE_REQUEST else MessageType.TASK_ASSIGNMENT,
            relatedTaskId = assignment.id
        )

        val responseMessage = ChatMessage(
            sender = "AI Task Agent",
            content = buildResponseMessage(chatResponse, assignment.chore),
            messageType = MessageType.TASK_ASSIGNMENT,
            relatedTaskId = assignment.id
        )

        _chatMessages.value = _chatMessages.value + listOf(requestMessage, responseMessage)
    }

    private fun buildResponseMessage(chatResponse: ChatResponse, chore: Chore): String {
        return buildString {
            append("✨ Task created!\n\n")
            append("📋 **${chatResponse.title ?: chore.name}**\n\n")
            append("${chatResponse.description ?: chore.description}\n\n")

            if (!chatResponse.checklist.isNullOrEmpty()) {
                append("✅ **Steps to Complete:**\n")
                chatResponse.checklist.forEach { item ->
                    append("• $item\n")
                }
                append("\n")
            }

            if (!chatResponse.photoGuidance.isNullOrEmpty()) {
                append("📸 **Photo Requirements:**\n")
                chatResponse.photoGuidance.forEach { guidance ->
                    append("• $guidance\n")
                }
                append("\n")
            }

            append("🏆 **Points Available:** ${chatResponse.pointsAvailable?.totalPossible ?: chore.points}\n")

            if (!chatResponse.encouragementMessage.isNullOrEmpty()) {
                append("\n💪 ${chatResponse.encouragementMessage}")
            }
        }
    }

    // ========== FALLBACK LOCAL CREATION ==========

    private suspend fun createLocalChoreAssignment(
        chore: Chore?,
        customDescription: String?,
        senderName: String,
        childName: String,
        isVoiceInput: Boolean
    ): Result<ChoreAssignment> {
        return try {
            val finalChore = chore ?: Chore(
                id = System.currentTimeMillis().toInt(),
                name = extractChoreTitle(customDescription ?: "Custom Task"),
                description = customDescription ?: "Custom task",
                icon = Icons.Default.Assignment,
                points = 5,
                category = ChoreCategory.CUSTOM,
                isCustom = true,
                createdBy = senderName
            )

            val assignment = ChoreAssignment(
                id = "local_${System.currentTimeMillis()}",
                chore = finalChore,
                assignedTo = childName,
                assignedBy = senderName,
                status = TaskStatus.PENDING,
                comments = "Created locally (offline mode)"
            )

            _taskAssignments.value = _taskAssignments.value + assignment

            val message = ChatMessage(
                sender = "Task System",
                content = "✅ Task created (offline): ${finalChore.name}\n\n⚠️ Created locally due to connection issue",
                messageType = MessageType.TASK_ASSIGNMENT,
                relatedTaskId = assignment.id
            )

            _chatMessages.value = _chatMessages.value + message
            _apiStatus.value = ApiStatus.Success

            Result.success(assignment)
        } catch (e: Exception) {
            _apiStatus.value = ApiStatus.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    // ========== CONVENIENCE METHODS FOR BACKWARD COMPATIBILITY ==========

    suspend fun sendParentTaskRequest(
        chore: Chore,
        childName: String,
        parentId: String = "parent_admin"
    ): Result<ChoreAssignment> {
        val senderName = parentId.replace("parent_", "").replaceFirstChar { it.uppercase() }
        return sendChoreToN8n(
            chore = chore,
            customDescription = null,
            senderName = senderName,
            childName = childName,
            isVoiceInput = false
        )
    }

    suspend fun sendCustomChoreRequestToN8n(
        customChoreDescription: String,
        senderName: String,
        childName: String,
        isVoiceInput: Boolean = false
    ): Result<ChatMessage> {
        val result = sendChoreToN8n(
            chore = null,
            customDescription = customChoreDescription,
            senderName = senderName,
            childName = childName,
            isVoiceInput = isVoiceInput
        )

        return result.map { assignment ->
            _chatMessages.value.lastOrNull { it.relatedTaskId == assignment.id }
                ?: ChatMessage(
                    sender = "System",
                    content = "Task created: ${assignment.chore.name}",
                    messageType = MessageType.TASK_ASSIGNMENT,
                    relatedTaskId = assignment.id
                )
        }
    }

    // ========== OTHER EXISTING METHODS (Keep unchanged) ==========

    suspend fun submitTaskWithImages(
        assignmentId: String,
        childId: String,
        imageUris: List<Uri>,
        completionMessage: String
    ): Result<ValidationResponse> = withContext(Dispatchers.IO) {
        // ... keep existing implementation
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

    suspend fun sendChatMessage(
        message: String,
        senderName: String,
        messageType: MessageType = MessageType.GENERAL,
        relatedTaskId: String? = null,
        images: List<Uri> = emptyList()
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            _apiStatus.value = ApiStatus.Loading

            val chatMessage = ChatMessage(
                sender = senderName,
                content = message,
                messageType = messageType,
                relatedTaskId = relatedTaskId,
                images = images.map { it.toString() }
            )

            _chatMessages.value = _chatMessages.value + chatMessage

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

    // ========== UTILITY METHODS ==========

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