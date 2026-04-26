package com.neuralnodes.inbox.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.models.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for conversation detail screen
 * Exact match to iOS SDK ConversationDetailViewModel
 */
class ConversationDetailViewModel(
    val conversationId: String,
    val conversationStatus: String,
    private val sdk: NeuralNodesInbox
) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending
    
    private val _showError = MutableStateFlow(false)
    val showError: StateFlow<Boolean> = _showError
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore
    
    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages
    
    private val _scrollToMessageId = MutableStateFlow<String?>(null)
    val scrollToMessageId: StateFlow<String?> = _scrollToMessageId
    
    private val pageSize = 15
    private var currentOffset = 0
    private var isInitialLoad = true
    
    fun startListening() {
        val realtimeClient = sdk.getRealtimeClient()
        viewModelScope.launch {
            realtimeClient.subscribeToConversation(conversationId).collect { message ->
                // Check if message already exists (avoid duplicates)
                if (!_messages.value.any { it.id == message.id }) {
                    _messages.value = _messages.value + listOf(message)
                    _scrollToMessageId.value = message.id
                }
            }
        }
    }
    
    fun stopListening() {
        val realtimeClient = sdk.getRealtimeClient()
        realtimeClient.unsubscribe(conversationId)
    }
    
    fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            currentOffset = 0
            _hasMoreMessages.value = true
            isInitialLoad = true
            
            try {
                val apiClient = sdk.getAPIClient()
                val fetchedMessages = apiClient.getMessages(
                    conversationId = conversationId,
                    limit = pageSize,
                    offset = currentOffset
                )
                
                _messages.value = fetchedMessages.sortedBy { it.createdAt }
                _hasMoreMessages.value = fetchedMessages.size == pageSize
                currentOffset = pageSize
                _isLoading.value = false
                
                // Scroll to last message
                fetchedMessages.lastOrNull()?.let {
                    _scrollToMessageId.value = it.id
                }
                
                // Mark as no longer initial load after delay
                viewModelScope.launch {
                    kotlinx.coroutines.delay(1000)
                    isInitialLoad = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
                _showError.value = true
                _isLoading.value = false
            }
        }
    }
    
    fun loadMoreMessages() {
        if (_isLoadingMore.value || !_hasMoreMessages.value || isInitialLoad) {
            return
        }
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            
            try {
                val apiClient = sdk.getAPIClient()
                val fetchedMessages = apiClient.getMessages(
                    conversationId = conversationId,
                    limit = pageSize,
                    offset = currentOffset
                )
                
                val existingIds = _messages.value.map { it.id }.toSet()
                val newMessages = fetchedMessages.filter { it.id !in existingIds }
                val sortedNewMessages = newMessages.sortedBy { it.createdAt }
                
                _messages.value = sortedNewMessages + _messages.value
                
                _hasMoreMessages.value = fetchedMessages.size == pageSize
                currentOffset += fetchedMessages.size
                _isLoadingMore.value = false
            } catch (e: Exception) {
                _isLoadingMore.value = false
            }
        }
    }
    
    fun setMessageText(text: String) {
        _messageText.value = text
    }
    
    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isEmpty()) return
        
        viewModelScope.launch {
            _messageText.value = ""
            _isSending.value = true
            
            // Create optimistic message
            val optimisticMessage = Message(
                id = "temp-${UUID.randomUUID()}",
                conversationId = conversationId,
                messageType = "text",
                messageText = text,
                senderType = "agent",
                senderName = "You",
                senderId = null,
                attachmentUrl = null,
                attachmentType = null,
                attachmentName = null,
                isRead = false,
                createdAt = Date()
            )
            
            _messages.value = _messages.value + optimisticMessage
            _scrollToMessageId.value = optimisticMessage.id
            
            try {
                val apiClient = sdk.getAPIClient()
                val sentMessage = apiClient.sendMessage(
                    conversationId = conversationId,
                    text = text
                )
                
                // Replace optimistic message with real one
                val index = _messages.value.indexOfFirst { it.id == optimisticMessage.id }
                if (index != -1) {
                    val updatedMessages = _messages.value.toMutableList()
                    updatedMessages[index] = sentMessage
                    _messages.value = updatedMessages
                    _scrollToMessageId.value = sentMessage.id
                }
                
                // Auto-change status from pending to active
                if (conversationStatus == "pending") {
                    apiClient.updateStatus(conversationId, "active")
                }
                
                _isSending.value = false
            } catch (e: Exception) {
                // Remove optimistic message on error
                _messages.value = _messages.value.filter { it.id != optimisticMessage.id }
                _messageText.value = text
                _errorMessage.value = e.localizedMessage
                _showError.value = true
                _isSending.value = false
            }
        }
    }
    
    fun markAsRead() {
        viewModelScope.launch {
            try {
                val apiClient = sdk.getAPIClient()
                apiClient.markAsRead(conversationId)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    fun updateStatus(status: String) {
        viewModelScope.launch {
            try {
                val apiClient = sdk.getAPIClient()
                apiClient.updateStatus(conversationId, status)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
                _showError.value = true
            }
        }
    }
}
