package com.neuralnodes.inbox.network

import com.neuralnodes.inbox.models.Escalation
import com.neuralnodes.inbox.models.ChatMessage

/**
 * API client for live chat escalations
 * Matches iOS LiveChatClient implementation
 */
class LiveChatClient(private val apiClient: APIClient) {
    
    // MARK: - Escalations
    
    suspend fun getEscalations(
        status: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<Escalation> {
        val params = mutableMapOf(
            "limit" to limit.toString(),
            "offset" to offset.toString()
        )
        
        status?.let { params["status"] = it }
        
        return apiClient.getEscalations(status, limit, offset)
    }
    
    suspend fun getEscalation(id: String): Escalation {
        return apiClient.getEscalation(id)
    }
    
    // MARK: - Messages
    
    suspend fun getEscalationMessages(
        escalationId: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<ChatMessage> {
        return apiClient.getEscalationMessages(escalationId, limit, offset)
    }
    
    suspend fun sendEscalationMessage(
        escalationId: String,
        text: String
    ): ChatMessage {
        return apiClient.sendEscalationMessage(escalationId, text)
    }
    
    suspend fun markEscalationMessagesRead(escalationId: String) {
        apiClient.markEscalationMessagesRead(escalationId)
    }
    
    // MARK: - Escalation Management
    
    suspend fun updateEscalationStatus(
        escalationId: String,
        status: String,
        resolutionNotes: String? = null
    ) {
        apiClient.updateEscalationStatus(escalationId, status, resolutionNotes)
    }
    
    suspend fun resolveEscalation(
        escalationId: String,
        notes: String? = null
    ) {
        updateEscalationStatus(escalationId, "resolved", notes)
    }
    
    suspend fun endEscalation(
        escalationId: String,
        reason: String? = null
    ) {
        updateEscalationStatus(escalationId, "closed", reason ?: "Chat ended by agent")
    }
    
    suspend fun transferEscalation(
        escalationId: String,
        toAgentId: String
    ) {
        apiClient.transferEscalation(escalationId, toAgentId)
    }
    
    suspend fun sendTypingIndicator(
        escalationId: String,
        isTyping: Boolean
    ) {
        apiClient.sendTypingIndicator(escalationId, isTyping)
    }
}
