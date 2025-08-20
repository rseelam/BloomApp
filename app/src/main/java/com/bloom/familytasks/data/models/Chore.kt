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
    val category: ChoreCategory
)

enum class ChoreCategory {
    CLEANING, KITCHEN, BEDROOM, OUTDOOR, PETS, ORGANIZATION
}

data class ChoreAssignment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chore: Chore,
    val assignedTo: String,
    val assignedBy: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val validationImages: List<String> = emptyList(),
    val comments: String = ""
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
    val messageType: MessageType
)

enum class MessageType {
    TASK_ASSIGNMENT, TASK_SUBMISSION, VALIDATION_REQUEST, VALIDATION_RESULT, GENERAL
}