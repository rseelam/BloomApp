package com.bloom.familytasks.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.bloom.familytasks.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TaskViewModel : ViewModel() {
    private val _choreAssignments = MutableStateFlow<List<ChoreAssignment>>(emptyList())
    val choreAssignments: StateFlow<List<ChoreAssignment>> = _choreAssignments.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedImages = mutableStateListOf<Uri>()
    val selectedImages: List<Uri> = _selectedImages

    var currentUser = mutableStateOf("Parent")

    fun assignChore(chore: Chore, assignedTo: String) {
        val assignment = ChoreAssignment(
            chore = chore,
            assignedTo = assignedTo,
            assignedBy = currentUser.value,
            status = TaskStatus.PENDING
        )

        _choreAssignments.value = _choreAssignments.value + assignment

        // Add chat message for assignment
        val message = ChatMessage(
            sender = currentUser.value,
            content = "New task assigned: ${chore.name} to $assignedTo",
            messageType = MessageType.TASK_ASSIGNMENT
        )
        _chatMessages.value = _chatMessages.value + message
    }

    fun submitTaskWithImages(assignmentId: String, imageUris: List<Uri>) {
        val assignment = _choreAssignments.value.find { it.id == assignmentId }
        assignment?.let {
            val updatedAssignment = it.copy(
                status = TaskStatus.SUBMITTED,
                validationImages = imageUris.map { uri -> uri.toString() }
            )

            _choreAssignments.value = _choreAssignments.value.map { a ->
                if (a.id == assignmentId) updatedAssignment else a
            }

            // Add chat message for submission
            val message = ChatMessage(
                sender = currentUser.value,
                content = "Task '${it.chore.name}' submitted for validation",
                images = imageUris.map { uri -> uri.toString() },
                messageType = MessageType.TASK_SUBMISSION
            )
            _chatMessages.value = _chatMessages.value + message
        }
    }

    fun validateTask(assignmentId: String, approved: Boolean, comments: String = "") {
        val assignment = _choreAssignments.value.find { it.id == assignmentId }
        assignment?.let {
            val newStatus = if (approved) TaskStatus.VALIDATED else TaskStatus.REJECTED
            val updatedAssignment = it.copy(
                status = newStatus,
                comments = comments
            )

            _choreAssignments.value = _choreAssignments.value.map { a ->
                if (a.id == assignmentId) updatedAssignment else a
            }

            // Add chat message for validation
            val message = ChatMessage(
                sender = "Validation Agent",
                content = if (approved)
                    "Task '${it.chore.name}' has been validated! Great job! 🎉"
                else
                    "Task '${it.chore.name}' needs more work. $comments",
                messageType = MessageType.VALIDATION_RESULT
            )
            _chatMessages.value = _chatMessages.value + message
        }
    }

    fun addSelectedImage(uri: Uri) {
        if (_selectedImages.size < 5) { // Limit to 5 images
            _selectedImages.add(uri)
        }
    }

    fun removeSelectedImage(uri: Uri) {
        _selectedImages.remove(uri)
    }

    fun clearSelectedImages() {
        _selectedImages.clear()
    }

    fun switchUser(user: String) {
        currentUser.value = user
    }

    fun getAssignmentsForChild(childName: String): List<ChoreAssignment> {
        return _choreAssignments.value.filter { it.assignedTo == childName }
    }

    fun getPendingValidations(): List<ChoreAssignment> {
        return _choreAssignments.value.filter { it.status == TaskStatus.SUBMITTED }
    }
}