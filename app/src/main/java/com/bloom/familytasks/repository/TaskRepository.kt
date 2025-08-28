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
import androidx.compose.material.icons.filled.CleaningServices
import com.bloom.familytasks.data.models.*
import com.bloom.familytasks.network.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject

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
        chore: Chore? = null,
        customDescription: String? = null,
        senderName: String,
        childName: String,
        isVoiceInput: Boolean = false
    ): Result<ChoreAssignment> = withContext(Dispatchers.IO) {
        try {
            _apiStatus.value = ApiStatus.Loading

            // Build message for N8N
            val choreMessage = when {
                chore != null -> "${chore.name}: ${chore.description}"
                customDescription != null -> "CUSTOM_CHORE: $customDescription"
                else -> throw IllegalArgumentException("Either chore or customDescription must be provided")
            }

            val chatInputData = ChatInputData(
                message = choreMessage,
                senderId = "parent_${senderName.lowercase()}",
                childName = childName,
                taskId = "task_${System.currentTimeMillis()}_$childName",
                parentTaskMessage = choreMessage
            )

            val request = ChatRequest(chatInput = gson.toJson(chatInputData))

            Log.d("TaskRepository", "=== SENDING TO N8N ===")
            Log.d("TaskRepository", "Request: ${request.chatInput}")

            val response = apiService.sendChatMessage(
                webhookId = NetworkModule.WEBHOOK_ID,
                request = request
            )

            if (response.isSuccessful) {
                val rawResponse = response.body()?.string() ?: "{}"
                Log.d("TaskRepository", "=== N8N RAW RESPONSE ===")
                Log.d("TaskRepository", "Response (first 500 chars): ${rawResponse.take(500)}")

                // Parse the N8N response
                val chatResponse = parseN8nResponse(rawResponse)

                // Build comments from the parsed response
                val comments = buildChoreComments(chatResponse, isVoiceInput)

                // If no checklist from N8N, provide task-specific defaults
                val finalComments = if (comments.isBlank() || !chatResponse.checklist.isNullOrEmpty() == false) {
                    Log.w("TaskRepository", "No checklist from N8N, using task-specific defaults")
                    when {
                        chore?.name?.contains("vacuum", ignoreCase = true) == true -> {
                            """
                        • Pick up any toys, papers, or items from the carpet so the vacuum can reach all spots
                        • Plug in the vacuum cleaner safely and turn it on
                        • Vacuum all carpeted areas in the living room by moving the vacuum slowly back and forth
                        • Empty the vacuum bag if it gets full
                        • Put the vacuum away when done
                        
                        ⚠️ Safety:
                        • Make sure the vacuum cord is out of the way to avoid tripping
                        • Do not put fingers or objects near the vacuum's moving parts
                        
                        📸 Photos Needed:
                        • Take a before photo showing the carpeted floor
                        • Take an after photo showing the clean carpet
                        
                        💪 Great job helping keep our living room clean and comfy!
                        """.trimIndent()
                        }
                        else -> comments.ifBlank {
                            """
                        • Complete the task as described
                        • Make sure to do a thorough job
                        • Take before and after photos
                        """.trimIndent()
                        }
                    }
                } else {
                    comments
                }

                Log.d("TaskRepository", "Final comments length: ${finalComments.length}")

                // Create the chore
                val finalChore = chore ?: Chore(
                    id = System.currentTimeMillis().toInt(),
                    name = chatResponse.title ?: extractChoreTitle(customDescription ?: "Custom Task"),
                    description = chatResponse.description ?: customDescription ?: "Custom task",
                    icon = Icons.Default.Assignment,
                    points = chore?.points ?: 2,
                    category = chore?.category ?: ChoreCategory.CUSTOM,
                    isCustom = chore == null,
                    createdBy = senderName
                )

                // Create assignment with comments
                val assignment = ChoreAssignment(
                    id = chatResponse.taskId ?: "task_${System.currentTimeMillis()}",
                    chore = finalChore,
                    assignedTo = childName,
                    assignedBy = senderName,
                    status = TaskStatus.PENDING,
                    comments = finalComments
                )

                Log.d("TaskRepository", "=== ASSIGNMENT CREATED ===")
                Log.d("TaskRepository", "Assignment ID: ${assignment.id}")
                Log.d("TaskRepository", "Comments in assignment: ${assignment.comments.length} chars")

                // Update state
                _taskAssignments.value = _taskAssignments.value + assignment

                // Add chat message
                val message = ChatMessage(
                    sender = "Task Agent",
                    content = buildResponseMessage(chatResponse, finalChore),
                    messageType = MessageType.TASK_ASSIGNMENT,
                    relatedTaskId = assignment.id
                )
                _chatMessages.value = _chatMessages.value + message

                _apiStatus.value = ApiStatus.Success
                return@withContext Result.success(assignment)

            } else {
                Log.e("TaskRepository", "N8N API failed with code: ${response.code()}")
                _apiStatus.value = ApiStatus.Error("API call failed: ${response.code()}")

                // Create fallback assignment
                return@withContext createLocalFallbackAssignment(chore, customDescription, senderName, childName)
            }

        } catch (e: Exception) {
            Log.e("TaskRepository", "Error in sendChoreToN8n: ${e.message}", e)
            _apiStatus.value = ApiStatus.Error(e.message ?: "Unknown error")

            // Create fallback assignment
            return@withContext createLocalFallbackAssignment(chore, customDescription, senderName, childName)
        }
    }

    private fun generateFallbackSteps(
        chore: Chore?,
        customDescription: String?,
        chatResponse: ChatResponse
    ): String {
        val taskName = chatResponse.title ?: chore?.name ?: "Task"

        return when {
            taskName.contains("vacuum", ignoreCase = true) -> {
                """
            • Pick up any toys, papers, or items from the carpet
            • Plug in the vacuum cleaner safely and turn it on
            • Vacuum all carpeted areas slowly back and forth
            • Empty the vacuum bag if needed
            • Put the vacuum away when done
            
            ⚠️ Safety:
            • Make sure the vacuum cord is out of the way
            • Keep fingers away from moving parts
            
            📸 Photos Needed:
            • Take a before photo of the carpet
            • Take an after photo showing clean carpet
            
            💪 Great job keeping our home clean!
            """.trimIndent()
            }
            taskName.contains("dishes", ignoreCase = true) -> {
                """
            • Clear all dishes from the table
            • Rinse dishes to remove food
            • Load dishes into dishwasher properly
            • Add detergent and start dishwasher
            • Wipe down the sink when done
            
            ⚠️ Safety:
            • Handle knives and sharp items carefully
            • Use warm (not hot) water
            
            📸 Photos Needed:
            • Take a before photo of dirty dishes
            • Take an after photo of clean kitchen
            
            💪 Thanks for helping in the kitchen!
            """.trimIndent()
            }
            else -> {
                """
            • Read the task description carefully
            • Gather any supplies you need
            • Complete the task step by step
            • Check your work when done
            • Clean up any mess
            
            📸 Photos Needed:
            • Take a before photo
            • Take an after photo when complete
            
            💪 You're doing great!
            """.trimIndent()
            }
        }
    }

    // Create a simpler fallback assignment method
    private suspend fun createFallbackAssignment(
        chore: Chore?,
        customDescription: String?,
        senderName: String,
        childName: String,
        isVoiceInput: Boolean
    ): Result<ChoreAssignment> {
        val finalChore = chore ?: Chore(
            id = System.currentTimeMillis().toInt(),
            name = extractChoreTitle(customDescription ?: "Custom Task"),
            description = customDescription ?: "Custom task",
            icon = Icons.Default.Assignment,
            points = chore?.points ?: 2,
            category = chore?.category ?: ChoreCategory.CUSTOM,
            isCustom = chore == null,
            createdBy = senderName
        )

        // Generate appropriate steps based on task type
        val steps = generateFallbackSteps(chore, customDescription, ChatResponse())

        val assignment = ChoreAssignment(
            id = "local_${System.currentTimeMillis()}",
            chore = finalChore,
            assignedTo = childName,
            assignedBy = senderName,
            status = TaskStatus.PENDING,
            comments = steps  // Always include steps
        )

        Log.d("TaskRepository", "Created fallback assignment with steps: ${steps.length} chars")

        _taskAssignments.value = _taskAssignments.value + assignment

        val message = ChatMessage(
            sender = "Task System",
            content = "✅ Task created: ${finalChore.name}\n\n💰 Reward: $${finalChore.points}",
            messageType = MessageType.TASK_ASSIGNMENT,
            relatedTaskId = assignment.id
        )

        _chatMessages.value = _chatMessages.value + message
        _apiStatus.value = ApiStatus.Success

        return Result.success(assignment)
    }


    // ========== HELPER METHODS (REUSABLE) ==========

    private fun parseN8nResponse(rawResponse: String): ChatResponse {
        Log.d("TaskRepository", "=== PARSING N8N RESPONSE ===")
        Log.d("TaskRepository", "Raw response length: ${rawResponse.length}")

        try {
            // Extract the JSON from the response structure
            val outputJson = when {
                // Handle array response like [{"output": "..."}]
                rawResponse.trim().startsWith("[") -> {
                    Log.d("TaskRepository", "Response is an array")
                    try {
                        // Use Gson to parse the array
                        val type = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
                        val arrayResponse: List<Map<String, Any>> = gson.fromJson(rawResponse, type)

                        if (arrayResponse.isNotEmpty()) {
                            val firstElement = arrayResponse[0]
                            val output = firstElement["output"]?.toString() ?: "{}"
                            Log.d("TaskRepository", "Extracted output from array: ${output.take(200)}")
                            output
                        } else {
                            Log.e("TaskRepository", "Empty array response")
                            "{}"
                        }
                    } catch (e: Exception) {
                        Log.e("TaskRepository", "Failed to parse array with Gson: ${e.message}")
                        // Try with JSONArray as fallback
                        try {
                            val jsonArray = JSONArray(rawResponse)
                            if (jsonArray.length() > 0) {
                                val obj = jsonArray.getJSONObject(0)
                                val output = obj.optString("output", "{}")
                                Log.d("TaskRepository", "Extracted output with JSONArray: ${output.take(200)}")
                                output
                            } else {
                                "{}"
                            }
                        } catch (je: Exception) {
                            Log.e("TaskRepository", "JSONArray also failed: ${je.message}")
                            "{}"
                        }
                    }
                }
                // Handle object response
                rawResponse.trim().startsWith("{") && rawResponse.contains("\"output\"") -> {
                    Log.d("TaskRepository", "Response is an object with output field")
                    try {
                        val jsonObject = JSONObject(rawResponse)
                        val output = jsonObject.optString("output", rawResponse)
                        Log.d("TaskRepository", "Extracted output from object: ${output.take(200)}")
                        output
                    } catch (e: Exception) {
                        Log.e("TaskRepository", "Failed to parse object: ${e.message}")
                        rawResponse
                    }
                }
                // Already a task response
                else -> {
                    Log.d("TaskRepository", "Response appears to be direct task response")
                    rawResponse
                }
            }

            // Clean the extracted JSON string
            val cleanJson = outputJson
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\\/", "/")
                .trim()

            Log.d("TaskRepository", "Clean JSON (first 300 chars): ${cleanJson.take(300)}")

            // Parse the ChatResponse
            val response = gson.fromJson(JSONObject(outputJson).getJSONObject("task_details").toString(), ChatResponse::class.java)

            Log.d("TaskRepository", "=== SUCCESSFULLY PARSED N8N RESPONSE ===")
            Log.d("TaskRepository", "Title: ${response.title}")
            Log.d("TaskRepository", "Description: ${response.description}")
            Log.d("TaskRepository", "Checklist items: ${response.checklist?.size ?: 0}")
            response.checklist?.forEachIndexed { i, item ->
                Log.d("TaskRepository", "Checklist[$i]: $item")
            }
            Log.d("TaskRepository", "Safety reminders: ${response.safetyReminders?.size ?: 0}")
            Log.d("TaskRepository", "Photo guidance: ${response.photoGuidance?.size ?: 0}")
            Log.d("TaskRepository", "Encouragement: ${response.encouragementMessage}")

            return response

        } catch (e: Exception) {
            Log.e("TaskRepository", "Failed to parse N8N response: ${e.message}", e)
            Log.e("TaskRepository", "Raw response was: ${rawResponse.take(500)}")

            // Return empty response - let the caller handle defaults
            return ChatResponse(
                taskId = "error_${System.currentTimeMillis()}",
                title = null,
                description = null,
                checklist = null,
                safetyReminders = null,
                photoGuidance = null,
                encouragementMessage = null
            )
        }
    }

    private fun buildChoreComments(chatResponse: ChatResponse, isVoiceInput: Boolean): String {
        Log.d("TaskRepository", "=== BUILDING COMMENTS FROM N8N RESPONSE ===")
        Log.d("TaskRepository", "Checklist size: ${chatResponse.checklist?.size ?: 0}")

        val result = buildString {
            // Process checklist items from N8N response
            if (!chatResponse.checklist.isNullOrEmpty()) {
                Log.d("TaskRepository", "Adding ${chatResponse.checklist.size} checklist items from N8N")
                chatResponse.checklist.forEach { item ->
                    // Remove "Step X:" prefix if present
                    val cleanItem = item
                        .replace(Regex("^Step \\d+:"), "")
                        .replace(Regex("^Step \\d+\\."), "")
                        .trim()
                    append("• $cleanItem\n")
                    Log.d("TaskRepository", "Added checklist item: $cleanItem")
                }
            }

            // Add safety reminders if present
            if (!chatResponse.safetyReminders.isNullOrEmpty()) {
                append("\n⚠️ Safety:\n")
                chatResponse.safetyReminders.forEach { reminder ->
                    append("• $reminder\n")
                    Log.d("TaskRepository", "Added safety reminder: $reminder")
                }
            }

            // Add photo guidance if present
            if (!chatResponse.photoGuidance.isNullOrEmpty()) {
                append("\n📸 Photos Needed:\n")
                chatResponse.photoGuidance.forEach { guidance ->
                    append("• $guidance\n")
                    Log.d("TaskRepository", "Added photo guidance: $guidance")
                }
            }

            // Add bonus opportunities if present
            if (!chatResponse.bonusOpportunities.isNullOrEmpty()) {
                append("\n💰 Bonus Tips:\n")
                chatResponse.bonusOpportunities.forEach { bonus ->
                    append("• $bonus\n")
                }
            }

            // Add encouragement message if present
            if (!chatResponse.encouragementMessage.isNullOrEmpty()) {
                append("\n💪 ${chatResponse.encouragementMessage}")
            }

            // Voice indicator
            if (isVoiceInput) {
                append("\n\n🎤 Created via voice command")
            }
        }

        Log.d("TaskRepository", "Built comments with ${result.length} characters")
        Log.d("TaskRepository", "Final comments content:\n$result")

        // Only return empty string if truly nothing was built
        // This allows the caller to decide on fallback
        return result
    }

    fun extractStepsFromAssignment(assignment: ChoreAssignment): List<TaskStep> {
        val steps = mutableListOf<TaskStep>()
        val lines = assignment.comments.split("\n").filter { it.isNotBlank() }

        var currentSection = TaskStepSection.CHECKLIST

        lines.forEach { line ->
            val cleanLine = line.trim()
            when {
                cleanLine.contains("⚠️ Safety:") -> {
                    currentSection = TaskStepSection.SAFETY
                }
                cleanLine.contains("📸 Photos Needed:") -> {
                    currentSection = TaskStepSection.PHOTOS
                }
                cleanLine.contains("💰 Bonus Tips:") -> {
                    currentSection = TaskStepSection.BONUS
                }
                cleanLine.startsWith("•") -> {
                    steps.add(TaskStep(
                        text = cleanLine.substring(1).trim(),
                        section = currentSection
                    ))
                }
                cleanLine.contains("🎤") || cleanLine.contains("💪") -> {
                    steps.add(TaskStep(
                        text = cleanLine
                            .replace("🎤", "")
                            .replace("💪", "")
                            .trim(),
                        section = TaskStepSection.INFO
                    ))
                }
            }
        }

        return steps
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
            append("✨ Task Created Successfully!\n\n")
            append("📋 **${chatResponse.title ?: chore.name}**\n")
            append("${chatResponse.description ?: chore.description}\n\n")

            if (!chatResponse.checklist.isNullOrEmpty()) {
                append("✅ **Steps to Complete:**\n")
                chatResponse.checklist.forEachIndexed { index, item ->
                    // Clean up step formatting
                    val cleanItem = item.replace(Regex("^Step \\d+:"), "").trim()
                    append("${index + 1}. $cleanItem\n")
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

            if (!chatResponse.safetyReminders.isNullOrEmpty()) {
                append("⚠️ **Safety Reminders:**\n")
                chatResponse.safetyReminders.forEach { reminder ->
                    append("• $reminder\n")
                }
                append("\n")
            }

            // Show dollar reward
            append("💰 **Reward:** $${chore.points}")

            // Add estimated duration if available
            if (chatResponse.estimatedDuration != null) {
                append(" (⏱️ ~${chatResponse.estimatedDuration} minutes)")
            }
            append("\n")

            if (!chatResponse.bonusOpportunities.isNullOrEmpty()) {
                append("\n🌟 **Bonus Opportunities:**\n")
                chatResponse.bonusOpportunities.forEach { bonus ->
                    append("• $bonus\n")
                }
            }

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
                points = 2,  // Custom chores are also $2
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
                content = "✅ Task created (offline): ${finalChore.name}\n\n💰 Reward: $${finalChore.points}\n\n⚠️ Created locally due to connection issue",
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
        Log.d("TaskRepository", "=== SENDING PARENT TASK REQUEST ===")
        Log.d("TaskRepository", "Chore: ${chore.name}")

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
                        append("Money earned: $${assignment.chore.points}")
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

        // Remove markdown code block markers if present
        if (cleanedJson.startsWith("```json")) {
            cleanedJson = cleanedJson.substring(7)
        }
        if (cleanedJson.endsWith("```")) {
            cleanedJson = cleanedJson.substring(0, cleanedJson.length - 3)
        }

        // Remove any escape characters for quotes (common in nested JSON)
        cleanedJson = cleanedJson.replace("\\\"", "\"")
        cleanedJson = cleanedJson.replace("\\n", "\n")
        cleanedJson = cleanedJson.replace("\\t", "\t")

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

    fun resetApiStatus() {
        _apiStatus.value = ApiStatus.Idle
    }

    fun clearAllAssignments() {
        _taskAssignments.value = emptyList()
    }

    private fun createLocalFallbackAssignment(
        chore: Chore?,
        customDescription: String?,
        senderName: String,
        childName: String
    ): Result<ChoreAssignment> {
        val finalChore = chore ?: Chore(
            id = System.currentTimeMillis().toInt(),
            name = "Custom Task",
            description = customDescription ?: "Complete this task",
            icon = Icons.Default.Assignment,
            points = 2,
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
            comments = """
            • Complete the task as described
            • Make sure to do a thorough job
            • Take before and after photos
        """.trimIndent()
        )

        _taskAssignments.value = _taskAssignments.value + assignment
        return Result.success(assignment)
    }
}

data class TaskStep(
    val text: String,
    val section: TaskStepSection
)

enum class TaskStepSection {
    CHECKLIST, SAFETY, PHOTOS, BONUS, INFO
}

sealed class ApiStatus {
    object Idle : ApiStatus()
    object Loading : ApiStatus()
    object Success : ApiStatus()
    data class Error(val message: String) : ApiStatus()
}