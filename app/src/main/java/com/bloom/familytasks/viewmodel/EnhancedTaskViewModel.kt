// app/src/main/java/com/bloom/familytasks/viewmodel/EnhancedTaskViewModel.kt
package com.bloom.familytasks.viewmodel

import android.app.Application
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bloom.familytasks.data.models.*
import com.bloom.familytasks.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

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

    // Voice recording states
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _voiceTranscription = MutableStateFlow("")
    val voiceTranscription: StateFlow<String> = _voiceTranscription

    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null

    // ========== UNIFIED METHOD FOR ALL CHORE ASSIGNMENTS ==========

    fun assignChore(
        chore: Chore? = null,
        customDescription: String? = null,
        childName: String = "Johnny",
        isVoiceInput: Boolean = false
    ) {
        viewModelScope.launch {
            repository.sendChoreToN8n(
                chore = chore,
                customDescription = customDescription,
                senderName = currentUser.value,
                childName = childName,
                isVoiceInput = isVoiceInput
            )
        }
    }

    // ========== CONVENIENCE METHODS FOR UI ==========

    fun assignChoreWithN8n(chore: Chore, childName: String) {
        assignChore(
            chore = chore,
            customDescription = null,
            childName = childName,
            isVoiceInput = false
        )
    }

    fun sendCustomChoreRequest(customChoreDescription: String, childName: String = "Johnny") {
        assignChore(
            chore = null,
            customDescription = customChoreDescription,
            childName = childName,
            isVoiceInput = false
        )
    }

    fun sendCustomChoreRequestToN8n(customChoreDescription: String, childName: String = "Johnny") {
        sendCustomChoreRequest(customChoreDescription, childName)
    }

    // ========== VOICE RECORDING METHODS ==========

    fun startVoiceRecording() {
        if (_isRecording.value) {
            stopVoiceRecording()
            return
        }

        try {
            val audioDir = File(getApplication<Application>().cacheDir, "audio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val audioFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            audioFilePath = audioFile.absolutePath

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(getApplication())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFilePath)

                try {
                    prepare()
                    start()
                    _isRecording.value = true
                    Log.d("VoiceRecording", "Recording started: $audioFilePath")
                } catch (e: IOException) {
                    Log.e("VoiceRecording", "Failed to start recording", e)
                    release()
                    mediaRecorder = null
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceRecording", "Error setting up recorder", e)
        }
    }

    fun stopVoiceRecording() {
        if (!_isRecording.value) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false

            audioFilePath?.let { path ->
                processVoiceRecording(path)
            }
        } catch (e: Exception) {
            Log.e("VoiceRecording", "Error stopping recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            _isRecording.value = false
        }
    }

    private fun processVoiceRecording(audioPath: String) {
        viewModelScope.launch {
            try {
                // In production, send to speech-to-text service
                val simulatedTranscription = generateSimulatedTranscription()
                _voiceTranscription.value = simulatedTranscription

                // Use unified method for voice chores
                assignChore(
                    chore = null,
                    customDescription = simulatedTranscription,
                    childName = "Johnny",
                    isVoiceInput = true
                )

                // Clean up the audio file
                File(audioPath).delete()
            } catch (e: Exception) {
                Log.e("VoiceProcessing", "Error processing voice", e)
            }
        }
    }

    private fun generateSimulatedTranscription(): String {
        val sampleChores = listOf(
            "Please clean your room and organize all your toys in the toy box",
            "Help set the table for dinner and put away the clean dishes",
            "Take out the trash and bring the bins back from the curb",
            "Vacuum the living room carpet and tidy up the cushions",
            "Feed the dog and give him fresh water"
        )
        return sampleChores.random()
    }

    // ========== CHAT AND MESSAGING ==========

    fun sendChatMessage(
        message: String,
        messageType: MessageType = MessageType.GENERAL,
        relatedTaskId: String? = null,
        images: List<Uri> = emptyList()
    ) {
        viewModelScope.launch {
            // Check if this is a custom chore request from parent
            if (currentUser.value.startsWith("Parent") &&
                (message.contains("chore", ignoreCase = true) ||
                        message.contains("task", ignoreCase = true) ||
                        message.contains("clean", ignoreCase = true) ||
                        message.contains("organize", ignoreCase = true))) {
                // Route to custom chore creation
                assignChore(
                    chore = null,
                    customDescription = message,
                    childName = "Johnny",
                    isVoiceInput = false
                )
            } else {
                // Regular chat message
                repository.sendChatMessage(
                    message = message,
                    senderName = currentUser.value,
                    messageType = messageType,
                    relatedTaskId = relatedTaskId,
                    images = images
                )
            }
        }
    }

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

    fun suggestChore(suggestion: String) {
        viewModelScope.launch {
            repository.sendChatMessage(
                message = suggestion,
                senderName = currentUser.value,
                messageType = MessageType.CHORE_SUGGESTION
            )
        }
    }

    // ========== TASK SUBMISSION AND VALIDATION ==========

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

    // ========== STATE MANAGEMENT ==========

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

    fun getMessagesForTask(taskId: String): List<ChatMessage> {
        return repository.getMessagesForTask(taskId)
    }

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

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) {
            stopVoiceRecording()
        }
    }
}