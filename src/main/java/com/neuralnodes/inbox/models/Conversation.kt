package com.neuralnodes.inbox.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Conversation(
    @SerializedName("id") val id: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("contact_name") val contactName: String?,
    @SerializedName("contact_email") val contactEmail: String?,
    @SerializedName("contact_phone") val contactPhone: String?,
    @SerializedName("last_message_preview") val lastMessagePreview: String?,
    @SerializedName("last_message_at") val lastMessageAt: Date?,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: Date,
    @SerializedName("updated_at") val updatedAt: Date
) {
    val channelIcon: String
        get() = when (channel) {
            "webchat" -> "💬"
            "whatsapp" -> "📱"
            "telegram" -> "✈️"
            "email" -> "📧"
            else -> "💬"
        }
    
    val displayName: String
        get() = contactName ?: contactEmail ?: contactPhone ?: "Unknown"
    
    val lastMessage: String?
        get() = lastMessagePreview
}

data class ConversationsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("conversations") val conversations: List<Conversation>,
    @SerializedName("count") val count: Int
)
