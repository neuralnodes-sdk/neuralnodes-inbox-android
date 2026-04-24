package com.neuralnodes.inbox.models

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Escalation model for Live Chat
 */
data class Escalation(
    @SerializedName("id") val id: String,
    @SerializedName("lead_id") val leadId: String?,
    @SerializedName("lead_name") val leadName: String?,
    @SerializedName("lead_email") val leadEmail: String?,
    @SerializedName("status") val status: EscalationStatus,
    @SerializedName("priority") val priority: String?,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("last_message_preview") val lastMessagePreviewRaw: String?,
    @SerializedName("conversation_history") val conversationHistory: List<ConversationHistoryItem>?,
    @SerializedName("last_message_at") val lastMessageAt: Date?,
    @SerializedName("created_at") val createdAt: Date,
    @SerializedName("updated_at") val updatedAt: Date,
    @SerializedName("resolved_at") val resolvedAt: Date?,
    @SerializedName("resolution_notes") val resolutionNotes: String?
) {
    val displayName: String
        get() = leadName ?: leadEmail ?: "Anonymous Visitor"
    
    val isActive: Boolean
        get() = status == EscalationStatus.ACTIVE
    
    val isPending: Boolean
        get() = status == EscalationStatus.PENDING
    
    val isResolved: Boolean
        get() = status == EscalationStatus.RESOLVED || status == EscalationStatus.CLOSED
    
    // Compute last message preview from conversation history
    val lastMessagePreview: String
        get() {
            // Try raw preview first
            if (!lastMessagePreviewRaw.isNullOrBlank()) {
                return lastMessagePreviewRaw
            }
            
            // Otherwise get from conversation history
            val lastMessage = conversationHistory?.lastOrNull()
            return when {
                lastMessage != null -> {
                    val prefix = if (lastMessage.sender == "user") "User: " else "Bot: "
                    val text = lastMessage.text.take(50)
                    "$prefix$text${if (lastMessage.text.length > 50) "..." else ""}"
                }
                else -> "No messages yet"
            }
        }
}

data class ConversationHistoryItem(
    @SerializedName("text") val text: String,
    @SerializedName("type") val type: String,
    @SerializedName("sender") val sender: String,
    @SerializedName("timestamp") val timestamp: String
)

enum class EscalationStatus {
    @SerializedName("active") ACTIVE,
    @SerializedName("pending") PENDING,
    @SerializedName("resolved") RESOLVED,
    @SerializedName("closed") CLOSED
}

data class EscalationsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("escalations") val escalations: List<Escalation>,
    @SerializedName("count") val count: Int
)

data class ChatMessage(
    @SerializedName("id") val id: String,
    @SerializedName("escalation_id") val escalationId: String,
    @SerializedName("message_type") val messageType: String,
    @SerializedName("message_text") val messageText: String,
    @SerializedName("sender_type") val senderType: String,
    @SerializedName("sender_name") val senderName: String?,
    @SerializedName("sender_id") val senderId: String?,
    @SerializedName("attachment_url") val attachmentUrl: String?,
    @SerializedName("attachment_type") val attachmentType: String?,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("read_at") val readAt: Date?,
    @SerializedName("created_at") val createdAt: Date
) {
    val isFromUser: Boolean get() = senderType == "user"
    val isFromAgent: Boolean get() = senderType == "agent"
    val displaySenderName: String get() = senderName ?: if (isFromUser) "Visitor" else "Agent"
}

data class ChatMessagesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("count") val count: Int
)

data class SendChatMessageRequest(
    @SerializedName("message_text") val messageText: String,
    @SerializedName("message_type") val messageType: String = "text"
)

data class SendChatMessageResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: ChatMessage
)

data class UpdateEscalationStatusRequest(
    @SerializedName("status") val status: String,
    @SerializedName("resolution_notes") val resolutionNotes: String? = null
)

data class TypingIndicator(
    @SerializedName("sender_name") val senderName: String,
    @SerializedName("sender_type") val senderType: String,
    @SerializedName("is_typing") val isTyping: Boolean
)

data class RealTimeAnalytics(
    @SerializedName("active_escalations") val activeEscalations: Int,
    @SerializedName("pending_escalations") val pendingEscalations: Int,
    @SerializedName("total_unread") val totalUnread: Int,
    @SerializedName("avg_response_time") val avgResponseTime: Double?
)
