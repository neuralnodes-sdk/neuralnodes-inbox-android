package com.neuralnodes.inbox.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralnodes.inbox.NeuralNodesInbox
import com.neuralnodes.inbox.models.ChatMessage
import com.neuralnodes.inbox.utils.NeuralNodesLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for Live Chat - Exact match to iOS SDK LiveChatViewModel
 */
class LiveChatViewModel(
    val escalationId: String,
    private val sdk: NeuralNodesInbox
) : ViewModel() {
    
    private val liveChatClient = sdk.getLiveChatClient()
    private val pusherClient = sdk.getPusherClient()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()
    
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    
    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()
    
    private val _scrollToMessageId = MutableStateFlow<String?>(null)
    val scrollToMessageId: StateFlow<String?> = _scrollToMessageId.asStateFlow()
    
    private val _currentStatus = MutableStateFlow("active")
    val currentStatus: StateFlow<String> = _currentStatus.asStateFlow()
    
    private val pageSize = 15
    private var currentOffset = 0
    private var isInitialLoad = true
    
    fun connect() {
        viewModelScope.launch {
            try {
                delay(500)
                _isConnected.value = true
                
                // Subscribe to Pusher channel
                pusherClient?.subscribeToEscalation(escalationId, onMessage = { message ->
                    NeuralNodesLogger.info("[LIVE CHAT VM] Received message from Pusher: ${message.messageText}")
                    
                    // Check if message already exists by ID
                    if (_messages.value.any { it.id == message.id }) {
                        NeuralNodesLogger.warning("[LIVE CHAT VM] Message with ID ${message.id} already exists, skipping")
                        return@subscribeToEscalation
                    }
                    
                    // Also check for duplicates by content and timestamp (within 2 seconds)
                    val isDuplicate = _messages.value.any { existingMsg ->
                        existingMsg.messageText == message.messageText &&
                        existingMsg.senderType == message.senderType &&
                        kotlin.math.abs(existingMsg.createdAt.time - message.createdAt.time) < 2000
                    }
                    
                    if (isDuplicate) {
                        NeuralNodesLogger.warning("[LIVE CHAT VM] Duplicate message detected by content/timestamp, skipping")
                        return@subscribeToEscalation
                    }
                    
                    NeuralNodesLogger.info("[LIVE CHAT VM] Adding message to list")
                    _messages.value = _messages.value + message
                    _scrollToMessageId.value = message.id
                    NeuralNodesLogger.info("[LIVE CHAT VM] Message added, total messages: ${_messages.value.size}")
                }, onTyping = { isTyping ->
                    _isTyping.value = isTyping
                })
            } catch (e: Exception) {
                _isConnected.value = false
            }
        }
    }
    
    fun disconnect() {
        pusherClient?.unsubscribeFromEscalation(escalationId)
        _isConnected.value = false
    }
    
    fun loadMessages() {
        viewModelScope.launch {
            NeuralNodesLogger.info("[LIVE CHAT VM] loadMessages started for escalation: $escalationId")
            currentOffset = 0
            _hasMoreMessages.value = true
            isInitialLoad = true
            
            try {
                // Load escalation details to get current status
                NeuralNodesLogger.info("[LIVE CHAT VM] Fetching escalation details...")
                val escalation = liveChatClient.getEscalation(escalationId)
                _currentStatus.value = escalation.status
                NeuralNodesLogger.info("[LIVE CHAT VM] Escalation status: ${escalation.status}")
                
                NeuralNodesLogger.info("[LIVE CHAT VM] Fetching messages...")
                val fetchedMessages = liveChatClient.getEscalationMessages(
                    escalationId = escalationId,
                    limit = pageSize,
                    offset = currentOffset
                )
                
                NeuralNodesLogger.info("[LIVE CHAT VM] Received ${fetchedMessages.size} messages")
                _messages.value = fetchedMessages.sortedBy { it.createdAt }
                _hasMoreMessages.value = fetchedMessages.size == pageSize
                currentOffset = pageSize
                
                // Mark messages as read
                NeuralNodesLogger.info("[LIVE CHAT VM] Marking messages as read...")
                liveChatClient.markEscalationMessagesRead(escalationId)
                NeuralNodesLogger.info("[LIVE CHAT VM] Messages marked as read")
                
                // Scroll to bottom after messages loaded
                fetchedMessages.lastOrNull()?.let {
                    _scrollToMessageId.value = it.id
                    NeuralNodesLogger.info("[LIVE CHAT VM] Scrolling to last message: ${it.id}")
                }
                
                // Mark as no longer initial load after delay
                viewModelScope.launch {
                    delay(1000)
                    isInitialLoad = false
                }
            } catch (e: Exception) {
                NeuralNodesLogger.error("[LIVE CHAT VM] Error loading messages", e)
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
                val fetchedMessages = liveChatClient.getEscalationMessages(
                    escalationId = escalationId,
                    limit = pageSize,
                    offset = currentOffset
                )
                
                // Filter out duplicates before inserting
                val existingIds = _messages.value.map { it.id }.toSet()
                val newMessages = fetchedMessages.filter { it.id !in existingIds }
                
                // Insert older messages at the beginning
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
            val optimisticMessage = ChatMessage(
                id = "temp-${UUID.randomUUID()}",
                escalationId = escalationId,
                messageType = "text",
                messageText = text,
                senderType = "agent",
                senderName = "You",
                senderId = null,
                attachmentUrl = null,
                attachmentType = null,
                attachmentName = null,
                isRead = false,
                readAt = null,
                createdAt = Date()
            )
            
            // Add optimistic message immediately
            _messages.value = _messages.value + optimisticMessage
            
            // Trigger scroll to new message
            _scrollToMessageId.value = optimisticMessage.id
            
            try {
                val sentMessage = liveChatClient.sendEscalationMessage(
                    escalationId = escalationId,
                    text = text
                )
                
                // Replace optimistic message with real one
                val index = _messages.value.indexOfFirst { it.id == optimisticMessage.id }
                if (index != -1) {
                    val updatedMessages = _messages.value.toMutableList()
                    updatedMessages[index] = sentMessage
                    _messages.value = updatedMessages
                    // Trigger scroll to real message
                    _scrollToMessageId.value = sentMessage.id
                }
                
                _isSending.value = false
            } catch (e: Exception) {
                // Remove optimistic message on error
                _messages.value = _messages.value.filter { it.id != optimisticMessage.id }
                _messageText.value = text
                _isSending.value = false
            }
        }
    }
    
    fun endChat(reason: String? = null) {
        viewModelScope.launch {
            try {
                liveChatClient.endEscalation(escalationId, reason)
                _currentStatus.value = "closed"
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    fun acceptChat() {
        viewModelScope.launch {
            try {
                liveChatClient.updateEscalationStatus(escalationId, "active")
                _currentStatus.value = "active"
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    fun resolveChat(notes: String? = null) {
        viewModelScope.launch {
            try {
                liveChatClient.resolveEscalation(escalationId, notes)
                _currentStatus.value = "resolved"
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    fun closeChat() {
        viewModelScope.launch {
            try {
                liveChatClient.updateEscalationStatus(escalationId, "closed")
                _currentStatus.value = "closed"
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    fun reopenChat() {
        viewModelScope.launch {
            try {
                liveChatClient.updateEscalationStatus(escalationId, "active")
                _currentStatus.value = "active"
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
