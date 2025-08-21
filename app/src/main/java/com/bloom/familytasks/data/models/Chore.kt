package com.bloom.familytasks.data.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class Chore(
    val id: Int,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val points: Int = 10,
    val category: ChoreCategory,
    val isCustom: Boolean = false,
    val createdBy: String? = null
)

enum class ChoreCategory {
    CLEANING, KITCHEN, BEDROOM, OUTDOOR, PETS, ORGANIZATION, CUSTOM
}

data class ChoreAssignment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chore: Chore,
    val assignedTo: String,
    val assignedBy: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val validationImages: List<String> = emptyList(),
    val comments: String = "",
    val chatMessages: List<ChatMessage> = emptyList()
)

enum class TaskStatus {
    PENDING, IN_PROGRESS, SUBMITTED, VALIDATED, REJECTED
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val content: String,
    val images: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: MessageType,
    val relatedTaskId: String? = null,
    val requiresResponse: Boolean = false
)

enum class MessageType {
    TASK_ASSIGNMENT, TASK_SUBMISSION, VALIDATION_REQUEST, VALIDATION_RESULT, GENERAL, CUSTOM_CHORE_REQUEST,
    CHORE_QUESTION,
    CHORE_SUGGESTION,
    HELP_REQUEST,
    CLARIFICATION
}