package com.bloom.familytasks.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.bloom.familytasks.data.models.*
import com.bloom.familytasks.network.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import android.util.Log
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
                val taskResponse = response.body()
                val rawResponse = response.body()?.string() ?: "No Body"

                Log.d("TaskReposiroty", "Raw response: $rawResponse")
                Log.d("TaskReposiroty", "Parsed response: $taskResponse")

                val jsonResponse = JSONObject(gson.fromJson(rawResponse, Map::class.java))

                val response = cleanJsonString(jsonResponse.getString("output"))
                Log.d("TaskReposiroty", "Raw response: json task_id " + response)
                val chatResponse = gson.fromJson(response, ChatResponse::class.java)
                Log.d("TaskReposiroty", "response task id"+ chatResponse.taskId)
                val assignment = ChoreAssignment(
                    id = chatResponse.taskId.toString(),
                    chore = chore,
                    assignedTo = chatResponse.childName.toString(),
                    assignedBy = parentId,
                    status = TaskStatus.PENDING,
                    comments = chatResponse.checklist.toString()
                )

                    _taskAssignments.value = _taskAssignments.value + assignment

                    // Add to chat
                    val message = ChatMessage(
                        sender = "Task Agent",
                        content = buildString {
                            append("📋 New Task: \n\n" + chatResponse.taskId)
                            append(chatResponse.description)
                            append("✅ Checklist:\n")
                            append(chatResponse.checklist)
                            append("\n🏆 Points Available: " + chatResponse.pointsAvailable)
                        },
                        messageType = MessageType.TASK_ASSIGNMENT
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

            // Convert images to base64
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
                // Parse validation response from the workflow
                val responseBody = response.body()

                // Update assignment status
                val updatedAssignment = assignment.copy(
                    status = TaskStatus.VALIDATED,
                    validationImages = imageUris.map { it.toString() }
                )

                _taskAssignments.value = _taskAssignments.value.map { a ->
                    if (a.id == assignmentId) updatedAssignment else a
                }

                // Add validation result to chat
                val message = ChatMessage(
                    sender = "Validation Agent",
                    content = buildString {
                        append("✨ Task Validated!\n\n")
                        append("Great job completing the task!\n")
                        append("Points earned: Check with parent for exact points")
                    },
                    messageType = MessageType.VALIDATION_RESULT
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

        // Remove markdown code block formatting if present
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

}

sealed class ApiStatus {
    object Idle : ApiStatus()
    object Loading : ApiStatus()
    object Success : ApiStatus()
    data class Error(val message: String) : ApiStatus()
}