// app/src/main/java/com/bloom/familytasks/viewmodel/EnhancedTaskViewModel.kt
package com.bloom.familytasks.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bloom.familytasks.data.models.*
import com.bloom.familytasks.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EnhancedTaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(application)

    val choreAssignments = repository.taskAssignments
    val chatMessages = repository.chatMessages
    val apiStatus = repository.apiStatus

    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages

    private val _currentUser = MutableStateFlow("Parent")
    val currentUser: StateFlow<String> = _currentUser

    private val _showChatDialog = MutableStateFlow(false)
    val showChatDialog: StateFlow<Boolean> = _showChatDialog

    private val _currentChatMessage = MutableStateFlow("")
    val currentChatMessage: StateFlow<String> = _currentChatMessage

    // NEW: Send custom chore request with proper parameters
    fun sendCustomChoreRequest(customChoreDescription: String, childName: String = "Johnny") {
        viewModelScope.launch {
            repository.sendCustomChoreRequest(
                customChoreDescription = customChoreDescription,
                senderName = currentUser.value,
                childName = childName
            )
        }
    }

    // NEW: Send general chat message
    fun sendChatMessage(
        message: String,
        messageType: MessageType = MessageType.GENERAL,
        relatedTaskId: String? = null,
        images: List<Uri> = emptyList()
    ) {
        viewModelScope.launch {
            repository.sendChatMessage(
                message = message,
                senderName = currentUser.value,
                messageType = messageType,
                relatedTaskId = relatedTaskId,
                images = images
            )
        }
    }

    // NEW: Send chore question (child asking about task)
    fun sendChoreQuestion(question: String, taskId: String? = null) {
        viewModelScope.launch {
            repository.sendChatMessage(
                message = question,
                senderName = currentUser.value,
                messageType = MessageType.CHORE_QUESTION,
                relatedTaskId = taskId
            )
        }
    }

    // NEW: Send help request
    fun sendHelpRequest(helpMessage: String, taskId: String? = null) {
        viewModelScope.launch {
            repository.sendChatMessage(
                message = helpMessage,
                senderName = currentUser.value,
                messageType = MessageType.HELP_REQUEST,
                relatedTaskId = taskId
            )
        }
    }

    // NEW: Child suggests a chore
    fun suggestChore(suggestion: String) {
        viewModelScope.launch {
            repository.sendChatMessage(
                message = suggestion,
                senderName = currentUser.value,
                messageType = MessageType.CHORE_SUGGESTION
            )
        }
    }

    // Existing methods...
    fun assignChoreWithN8n(chore: Chore, childName: String) {
        viewModelScope.launch {
            repository.sendParentTaskRequest(
                chore = chore,
                childName = childName,
                parentId = "parent_${currentUser.value.lowercase()}"
            )
        }
    }

    fun submitTaskWithN8n(assignmentId: String, imageUris: List<Uri>, message: String = "Task completed!") {
        viewModelScope.launch {
            val childName = currentUser.value.replace("child_", "")
            repository.submitTaskWithImages(
                assignmentId = assignmentId,
                childId = childName,
                imageUris = imageUris,
                completionMessage = message
            )
        }
    }

    fun addSelectedImage(uri: Uri) {
        if (_selectedImages.value.size < 5) {
            _selectedImages.value = _selectedImages.value + uri
        }
    }

    fun removeSelectedImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value.filter { it != uri }
    }

    fun clearSelectedImages() {
        _selectedImages.value = emptyList()
    }

    fun switchUser(user: String) {
        _currentUser.value = user
    }

    fun validateTask(assignmentId: String, approved: Boolean, comments: String = "") {
        viewModelScope.launch {
            val assignment = choreAssignments.value.find { it.id == assignmentId }
            assignment?.let {
                val newStatus = if (approved) TaskStatus.VALIDATED else TaskStatus.REJECTED
                val updatedAssignment = it.copy(
                    status = newStatus,
                    comments = comments
                )

                updateLocalAssignment(updatedAssignment)

                val message = ChatMessage(
                    sender = "Validation Agent",
                    content = if (approved)
                        "Task '${it.chore.name}' has been validated! Great job! 🎉"
                    else
                        "Task '${it.chore.name}' needs more work. $comments",
                    messageType = MessageType.VALIDATION_RESULT,
                    relatedTaskId = assignmentId
                )
                addChatMessage(message)
            }
        }
    }

    // NEW: Chat dialog controls
    fun showChatDialog() {
        _showChatDialog.value = true
    }

    fun hideChatDialog() {
        _showChatDialog.value = false
        _currentChatMessage.value = ""
    }

    fun updateCurrentChatMessage(message: String) {
        _currentChatMessage.value = message
    }

    // NEW: Get messages for specific task
    fun getMessagesForTask(taskId: String): List<ChatMessage> {
        return repository.getMessagesForTask(taskId)
    }

    // NEW: Clear all chat messages
    fun clearAllChatMessages() {
        repository.clearChatMessages()
    }

    private fun updateLocalAssignment(assignment: ChoreAssignment) {
        val currentAssignments = choreAssignments.value.toMutableList()
        val index = currentAssignments.indexOfFirst { it.id == assignment.id }
        if (index != -1) {
            currentAssignments[index] = assignment
            repository.updateAssignmentLocally(currentAssignments)
        }
    }

    private fun addChatMessage(message: ChatMessage) {
        repository.addChatMessage(message)
    }
}