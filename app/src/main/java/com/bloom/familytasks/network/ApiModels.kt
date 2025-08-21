// app/src/main/java/com/bloom/familytasks/network/ApiModels.kt
package com.bloom.familytasks.network

import com.google.gson.annotations.SerializedName

// Keep your existing working format - don't break what works!
data class ChatRequest(
    @SerializedName("chatInput")
    val chatInput: String
)

data class ChatInputData(
    @SerializedName("message")
    val message: String,

    @SerializedName("sender_id")  // Keep original working name
    val senderId: String,

    @SerializedName("child_name")  // Keep original working name
    val childName: String? = null,

    @SerializedName("task_id")     // Keep original working name
    val taskId: String? = null,

    @SerializedName("parent_task_message")  // Keep original working name
    val parentTaskMessage: String? = null,

    @SerializedName("images")
    val images: List<ImageAttachment>? = null,

    @SerializedName("sessionId")
    val sessionId: String = "session_${System.currentTimeMillis()}"
)

data class ImageAttachment(
    @SerializedName("data")
    val data: String? = null, // Base64 encoded image

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("filename")
    val filename: String? = null,

    @SerializedName("mimetype")
    val mimetype: String? = null,

    @SerializedName("size")
    val size: Long? = null
)

// Your existing response models - keep them unchanged
data class ChatResponse(
    @SerializedName("task_id")
    val taskId: String? = null,

    @SerializedName("child_name")
    val childName: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("checklist")
    val checklist: List<String>? = null,

    @SerializedName("validation_criteria")
    val validationCriteria: List<String>? = null,

    @SerializedName("safety_reminders")
    val safetyReminders: List<String>? = null,

    @SerializedName("quality_standards")
    val qualityStandards: List<String>? = null,

    @SerializedName("photo_guidance")
    val photoGuidance: List<String>? = null,

    @SerializedName("encouragement_message")
    val encouragementMessage: String? = null,

    @SerializedName("estimated_duration")
    val estimatedDuration: Int? = null,

    @SerializedName("difficulty_level")
    val difficultyLevel: String? = null,

    @SerializedName("points_available")
    val pointsAvailable: PointsBreakdown? = null,

    @SerializedName("bonus_opportunities")
    val bonusOpportunities: List<String>? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    // Optional - only if your workflow supports these
    @SerializedName("response_message")
    val responseMessage: String? = null
)

data class PointsBreakdown(
    @SerializedName("base_points")
    val basePoints: Int,

    @SerializedName("quality_points")
    val qualityPoints: Int,

    @SerializedName("bonus_points")
    val bonusPoints: Int,

    @SerializedName("total_possible")
    val totalPossible: Int
)

data class ValidationResponse(
    @SerializedName("validation_complete")
    val validationComplete: Boolean,

    @SerializedName("child_name")
    val childName: String,

    @SerializedName("task_title")
    val taskTitle: String,

    @SerializedName("total_points_earned")
    val totalPointsEarned: Double,

    @SerializedName("validation_status")
    val validationStatus: String,

    @SerializedName("feedback")
    val feedback: String,

    @SerializedName("achievements")
    val achievements: List<String>,

    @SerializedName("areas_done_well")
    val areasDoneWell: List<String>,

    @SerializedName("improvement_suggestions")
    val improvementSuggestions: List<String>,

    @SerializedName("completion_percentage")
    val completionPercentage: Int,

    @SerializedName("quality_rating")
    val qualityRating: String,

    @SerializedName("celebration")
    val celebration: String,

    @SerializedName("direct_images_processed")
    val imagesProcessed: List<ProcessedImage>
)

data class ProcessedImage(
    @SerializedName("file_id")
    val fileId: String,

    @SerializedName("filename")
    val filename: String,

    @SerializedName("analysis")
    val analysis: String,

    @SerializedName("validation_result")
    val validationResult: Map<String, Any>
)