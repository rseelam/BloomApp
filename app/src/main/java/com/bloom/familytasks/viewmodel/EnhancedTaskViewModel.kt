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

                // Update locally (since we don't have a direct validation endpoint)
                updateLocalAssignment(updatedAssignment)

                // Add chat message for validation result
                val message = ChatMessage(
                    sender = "Validation Agent",
                    content = if (approved)
                        "Task '${it.chore.name}' has been validated! Great job! 🎉"
                    else
                        "Task '${it.chore.name}' needs more work. $comments",
                    messageType = MessageType.VALIDATION_RESULT
                )
                addChatMessage(message)
            }
        }
    }

    // Helper method to update assignment locally
    private fun updateLocalAssignment(assignment: ChoreAssignment) {
        val currentAssignments = choreAssignments.value.toMutableList()
        val index = currentAssignments.indexOfFirst { it.id == assignment.id }
        if (index != -1) {
            currentAssignments[index] = assignment
            // This would need to be added to repository
            repository.updateAssignmentLocally(currentAssignments)
        }
    }

    // Helper method to add chat message
    private fun addChatMessage(message: ChatMessage) {
        repository.addChatMessage(message)
    }

}