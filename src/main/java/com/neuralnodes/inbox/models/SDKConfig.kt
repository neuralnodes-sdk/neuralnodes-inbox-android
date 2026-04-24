package com.neuralnodes.inbox.models

import com.google.gson.annotations.SerializedName

data class SDKConfig(
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("ably_key") val ablyKey: String?,
    @SerializedName("pusher_key") val pusherKey: String?,
    @SerializedName("pusher_cluster") val pusherCluster: String?,
    @SerializedName("api_base_url") val apiBaseURL: String,
    @SerializedName("features") val features: Features,
    @SerializedName("branding") val branding: Branding,
    @SerializedName("limits") val limits: Limits
) {
    data class Features(
        @SerializedName("push_notifications") val pushNotifications: Boolean,
        @SerializedName("file_upload") val fileUpload: Boolean,
        @SerializedName("voice_messages") val voiceMessages: Boolean,
        @SerializedName("video_messages") val videoMessages: Boolean,
        @SerializedName("typing_indicators") val typingIndicators: Boolean,
        @SerializedName("read_receipts") val readReceipts: Boolean,
        @SerializedName("conversation_search") val conversationSearch: Boolean
    )
    
    data class Branding(
        @SerializedName("primary_color") val primaryColor: String,
        @SerializedName("logo_url") val logoURL: String?,
        @SerializedName("text_color") val textColor: String? = null,
        @SerializedName("accent_color") val accentColor: String? = null,
        @SerializedName("secondary_color") val secondaryColor: String? = null,
        @SerializedName("background_color") val backgroundColor: String? = null
    )
    
    data class Limits(
        @SerializedName("max_file_size_mb") val maxFileSizeMB: Int,
        @SerializedName("max_message_length") val maxMessageLength: Int,
        @SerializedName("messages_per_page") val messagesPerPage: Int,
        @SerializedName("max_conversations_cache") val maxConversationsCache: Int
    )
}

data class SDKConfigResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("config") val config: SDKConfig
)
