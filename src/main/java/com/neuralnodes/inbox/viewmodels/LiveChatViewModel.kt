package com.neuralnodes.inbox.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.models.ChatMessage
import com.neuralnodes.inbox.models.Escalation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Live Chat - shared between Activity and Composable
 */
class LiveChatViewModel(
    private val escalationId: String,
    private val sdk: NeuralNodesInbox
) : ViewModel() {
    
    private val apiClient = sdk.getAPIClient()
    private val pusherClient = sdk.getPusherClient()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()
    
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _currentStatus = MutableStateFlow("active")
    val currentStatus: StateFlow<String> = _currentStatus.asStateFlow()
    
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()
    
    init {
        loadMessages()
        subscribeToEscalation()
    }
    
    fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val loadedMessages = apiClient.getEscalationMessages(escalationId, limit = 100)
                _messages.value = loadedMessages.sortedBy { it.createdAt }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load messages"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            try {
                // Optimistic update
                val optimisticMessage = ChatMessage(
                    id = "temp_${System.currentTimeMillis()}",
                    escalationId = escalationId,
                    messageType = "text",
                    messageText = text,
                    senderType = "agent",
                    senderName = "You",
                    senderId = null,
                    attachmentUrl = null,
                    attachmentType = null,
                    isRead = true,
                    readAt = null,
                    createdAt = java.util.Date()
                )
                
                _messages.value = _messages.value + optimisticMessage
                _messageText.value = ""
                
                // Send to server
                val serverMessage = apiClient.sendClientMessage(escalationId, text)
                
                // Replace optimistic with server message
                _messages.value = _messages.value.map {
                    if (it.id == optimisticMessage.id) serverMessage else it
                }
                
            } catch (e: Exception) {
                // Rollback on error
                _messages.value = _messages.value.filter { it.id != "temp_${System.currentTimeMillis()}" }
                _error.value = e.message ?: "Failed to send message"
                _messageText.value = text // Restore text
            }
        }
    }
    
    fun setMessageText(text: String) {
        _messageText.value = text
    }
    
    fun resolveChat() {
        viewModelScope.launch {
            try {
                apiClient.resolveEscalation(escalationId)
                _currentStatus.value = "resolved"
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to resolve chat"
            }
        }
    }
    
    fun endChat() {
        viewModelScope.launch {
            try {
                apiClient.endEscalation(escalationId)
                _currentStatus.value = "closed"
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to end chat"
            }
        }
    }
    
    fun reopenChat() {
        viewModelScope.launch {
            try {
                // API call to reopen
                _currentStatus.value = "active"
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to reopen chat"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    private fun subscribeToEscalation() {
        pusherClient?.let { client ->
            viewModelScope.launch {
                try {
                    client.subscribeToEscalation(
                        escalationId,
                        onNewMessage = { message ->
                            val currentMessages = _messages.value.toMutableList()
                            if (!currentMessages.any { it.id == message.id }) {
                                currentMessages.add(message)
                                _messages.value = currentMessages.sortedBy { it.createdAt }
                            }
                        },
                        onTypingIndicator = { indicator ->
                            _isTyping.value = indicator.isTyping
                        },
                        onStatusChanged = { data ->
                            _currentStatus.value = data["status"] as? String ?: "active"
                        }
                    ).collect { /* Handle events */ }
                } catch (e: Exception) {
                    _error.value = "Connection error: ${e.message}"
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        pusherClient?.unsubscribe("private-escalation-$escalationId")
    }
}
