package com.neuralnodes.inbox.models

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Search result models
 * Exact match to iOS SDK SearchModels.swift
 */

// MARK: - Search Conversation Result

data class SearchConversationResult(
    @SerializedName("id") val id: String,
    @SerializedName("client_id") val clientId: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("contact_name") val contactName: String?,
    @SerializedName("contact_email") val contactEmail: String?,
    @SerializedName("contact_phone") val contactPhone: String?,
    @SerializedName("contact_identifier") val contactIdentifier: String?,
    @SerializedName("status") val status: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("message_count") val messageCount: Int,
    @SerializedName("last_message_preview") val lastMessagePreview: String?,
    @SerializedName("last_message_at") val lastMessageAt: Date?,
    @SerializedName("relevance_score") val relevanceScore: Int
)

// MARK: - Search Message Result

data class SearchMessageResult(
    @SerializedName("id") val id: String,
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("message_text") val messageText: String,
    @SerializedName("sender_type") val senderType: String,
    @SerializedName("sender_name") val senderName: String?,
    @SerializedName("created_at") val createdAt: Date,
    @SerializedName("matched_text") val matchedText: String?,
    @SerializedName("previous_messages") val previousMessages: List<MessageContext>?,
    @SerializedName("next_messages") val nextMessages: List<MessageContext>?,
    
    // Additional fields for global search
    @SerializedName("contact_name") val contactName: String?,
    @SerializedName("contact_email") val contactEmail: String?,
    @SerializedName("contact_phone") val contactPhone: String?,
    @SerializedName("contact_identifier") val contactIdentifier: String?,
    @SerializedName("channel") val channel: String?,
    @SerializedName("conversation_status") val conversationStatus: String?
)

data class MessageContext(
    @SerializedName("id") val id: String,
    @SerializedName("message_text") val messageText: String,
    @SerializedName("sender_type") val senderType: String,
    @SerializedName("created_at") val createdAt: Date
)

// MARK: - Search Response Wrappers

data class SearchConversationsResponse(
    @SerializedName("results") val results: List<SearchConversationResult>,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("offset") val offset: Int,
    @SerializedName("has_more") val hasMore: Boolean,
    @SerializedName("error") val error: String?
)

data class SearchMessagesResponse(
    @SerializedName("results") val results: List<SearchMessageResult>,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("offset") val offset: Int,
    @SerializedName("has_more") val hasMore: Boolean
)

data class SearchSuggestionsResponse(
    @SerializedName("suggestions") val suggestions: List<String>
)
