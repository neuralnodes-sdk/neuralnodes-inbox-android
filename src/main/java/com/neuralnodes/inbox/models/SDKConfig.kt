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
    @SerializedName("ui_customization") val uiCustomization: UICustomization,
    @SerializedName("limits") val limits: Limits
) {
    data class Features(
        @SerializedName("push_notifications") val pushNotifications: Boolean,
        @SerializedName("file_upload") val fileUpload: Boolean,
        @SerializedName("voice_messages") val voiceMessages: Boolean,
        @SerializedName("video_messages") val videoMessages: Boolean,
        @SerializedName("typing_indicators") val typingIndicators: Boolean,
        @SerializedName("read_receipts") val readReceipts: Boolean,
        @SerializedName("conversation_search") val conversationSearch: Boolean,
        @SerializedName("emoji_reactions") val emojiReactions: Boolean,
        @SerializedName("message_forwarding") val messageForwarding: Boolean,
        @SerializedName("conversation_export") val conversationExport: Boolean,
        @SerializedName("dark_mode") val darkMode: Boolean,
        @SerializedName("auto_translate") val autoTranslate: Boolean
    )
    
    data class Branding(
        @SerializedName("primary_color") val primaryColor: String,
        @SerializedName("logo_url") val logoURL: String?,
        @SerializedName("text_color") val textColor: String? = null,
        @SerializedName("accent_color") val accentColor: String? = null,
        @SerializedName("secondary_color") val secondaryColor: String? = null,
        @SerializedName("background_color") val backgroundColor: String? = null,
        @SerializedName("surface_color") val surfaceColor: String? = null,
        @SerializedName("error_color") val errorColor: String? = null,
        @SerializedName("success_color") val successColor: String? = null,
        @SerializedName("warning_color") val warningColor: String? = null,
        @SerializedName("company_name") val companyName: String? = null,
        @SerializedName("welcome_message") val welcomeMessage: String? = null
    )
    
    data class UICustomization(
        // Layout & Structure
        @SerializedName("show_toolbar") val showToolbar: Boolean = true,
        @SerializedName("show_status_bar") val showStatusBar: Boolean = true,
        @SerializedName("show_connection_status") val showConnectionStatus: Boolean = true,
        @SerializedName("show_typing_indicator") val showTypingIndicator: Boolean = true,
        @SerializedName("show_message_timestamps") val showMessageTimestamps: Boolean = true,
        @SerializedName("show_sender_names") val showSenderNames: Boolean = true,
        @SerializedName("show_message_status") val showMessageStatus: Boolean = true,
        @SerializedName("show_unread_count") val showUnreadCount: Boolean = true,
        @SerializedName("show_conversation_preview") val showConversationPreview: Boolean = true,
        
        // Message Bubbles
        @SerializedName("message_bubble_style") val messageBubbleStyle: String = "rounded", // rounded, square, minimal
        @SerializedName("message_bubble_elevation") val messageBubbleElevation: Int = 2,
        @SerializedName("user_message_alignment") val userMessageAlignment: String = "right", // left, right, center
        @SerializedName("agent_message_alignment") val agentMessageAlignment: String = "left",
        @SerializedName("message_spacing") val messageSpacing: Int = 8,
        @SerializedName("bubble_corner_radius") val bubbleCornerRadius: Int = 16,
        
        // Input Field
        @SerializedName("input_field_style") val inputFieldStyle: String = "rounded", // rounded, square, underline
        @SerializedName("input_placeholder_text") val inputPlaceholderText: String = "Type a message...",
        @SerializedName("send_button_style") val sendButtonStyle: String = "icon", // icon, text, floating
        @SerializedName("show_attachment_button") val showAttachmentButton: Boolean = true,
        @SerializedName("show_emoji_button") val showEmojiButton: Boolean = true,
        @SerializedName("input_max_lines") val inputMaxLines: Int = 4,
        
        // List Styles
        @SerializedName("conversation_item_style") val conversationItemStyle: String = "card", // card, list, minimal
        @SerializedName("show_conversation_avatars") val showConversationAvatars: Boolean = true,
        @SerializedName("show_channel_icons") val showChannelIcons: Boolean = true,
        @SerializedName("show_priority_badges") val showPriorityBadges: Boolean = true,
        @SerializedName("show_status_chips") val showStatusChips: Boolean = true,
        
        // Animations & Effects
        @SerializedName("enable_animations") val enableAnimations: Boolean = true,
        @SerializedName("message_animation_type") val messageAnimationType: String = "slide", // slide, fade, bounce
        @SerializedName("loading_animation_type") val loadingAnimationType: String = "spinner", // spinner, dots, pulse
        @SerializedName("transition_duration") val transitionDuration: Int = 300,
        
        // Typography
        @SerializedName("font_family") val fontFamily: String = "default", // default, roboto, opensans, custom
        @SerializedName("message_text_size") val messageTextSize: Int = 16,
        @SerializedName("timestamp_text_size") val timestampTextSize: Int = 12,
        @SerializedName("sender_name_text_size") val senderNameTextSize: Int = 14,
        @SerializedName("title_text_size") val titleTextSize: Int = 18,
        
        // Spacing & Padding
        @SerializedName("screen_padding") val screenPadding: Int = 16,
        @SerializedName("message_padding") val messagePadding: Int = 12,
        @SerializedName("list_item_padding") val listItemPadding: Int = 16,
        @SerializedName("section_spacing") val sectionSpacing: Int = 24,
        
        // Empty States
        @SerializedName("empty_inbox_title") val emptyInboxTitle: String = "No conversations yet",
        @SerializedName("empty_inbox_message") val emptyInboxMessage: String = "Start a conversation to see it here",
        @SerializedName("empty_chat_title") val emptyChatTitle: String = "No escalations",
        @SerializedName("empty_chat_message") val emptyChatMessage: String = "Live chat escalations will appear here",
        @SerializedName("show_empty_state_icons") val showEmptyStateIcons: Boolean = true,
        
        // Toolbar Customization
        @SerializedName("toolbar_style") val toolbarStyle: String = "elevated", // elevated, flat, transparent
        @SerializedName("show_back_button") val showBackButton: Boolean = true,
        @SerializedName("show_menu_button") val showMenuButton: Boolean = true,
        @SerializedName("toolbar_title_alignment") val toolbarTitleAlignment: String = "left", // left, center
        
        // Status & Indicators
        @SerializedName("online_indicator_color") val onlineIndicatorColor: String = "#10b981",
        @SerializedName("offline_indicator_color") val offlineIndicatorColor: String = "#ef4444",
        @SerializedName("typing_indicator_style") val typingIndicatorStyle: String = "dots", // dots, text, pulse
        @SerializedName("read_receipt_style") val readReceiptStyle: String = "checkmarks", // checkmarks, text, icons
        
        // Custom Strings
        @SerializedName("resolve_button_text") val resolveButtonText: String = "Resolve",
        @SerializedName("reopen_button_text") val reopenButtonText: String = "Reopen",
        @SerializedName("send_button_text") val sendButtonText: String = "Send",
        @SerializedName("retry_button_text") val retryButtonText: String = "Retry",
        @SerializedName("loading_text") val loadingText: String = "Loading...",
        @SerializedName("connecting_text") val connectingText: String = "Connecting...",
        @SerializedName("offline_text") val offlineText: String = "Offline"
    )
    
    data class Limits(
        @SerializedName("max_file_size_mb") val maxFileSizeMB: Int,
        @SerializedName("max_message_length") val maxMessageLength: Int,
        @SerializedName("messages_per_page") val messagesPerPage: Int,
        @SerializedName("max_conversations_cache") val maxConversationsCache: Int,
        @SerializedName("auto_refresh_interval") val autoRefreshInterval: Int = 30000,
        @SerializedName("typing_timeout") val typingTimeout: Int = 3000,
        @SerializedName("connection_timeout") val connectionTimeout: Int = 10000
    )
}

data class SDKConfigResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("config") val config: SDKConfig
)
