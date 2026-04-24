package com.neuralnodes.inbox.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Message(
    @SerializedName("id") val id: String,
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("message_type") val messageType: String,
    @SerializedName("message_text") val messageText: String,
    @SerializedName("sender_type") val senderType: String,
    @SerializedName("sender_name") val senderName: String?,
    @SerializedName("sender_id") val senderId: String?,
    @SerializedName("attachment_url") val attachmentUrl: String?,
    @SerializedName("attachment_type") val attachmentType: String?,
    @SerializedName("attachment_name") val attachmentName: String?,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: Date
) {
    val isFromUser: Boolean get() = senderType == "user"
    val isFromAgent: Boolean get() = senderType == "agent"
    val displaySenderName: String get() = senderName ?: if (isFromUser) "User" else "Agent"
}

data class MessagesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("count") val count: Int
)

data class SendMessageRequest(
    @SerializedName("message_text") val messageText: String,
    @SerializedName("message_type") val messageType: String = "text",
    @SerializedName("attachment_url") val attachmentUrl: String? = null,
    @SerializedName("attachment_type") val attachmentType: String? = null,
    @SerializedName("attachment_name") val attachmentName: String? = null
)

data class SendMessageResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: Message
)
