package com.neuralnodes.inbox.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.neuralnodes.inbox.models.SDKConfig
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

/**
 * Utility class to apply UI customizations from SDK configuration
 */
object UICustomizer {
    
    /**
     * Apply branding colors to a view
     */
    fun applyBranding(view: View, config: SDKConfig) {
        val branding = config.branding
        val uiCustomization = config.uiCustomization
        
        when (view) {
            is MaterialCardView -> {
                applyCardStyling(view, branding, uiCustomization)
            }
            is MaterialButton -> {
                applyButtonStyling(view, branding, uiCustomization)
            }
            is TextView -> {
                applyTextStyling(view, branding, uiCustomization)
            }
            is TextInputLayout -> {
                applyInputStyling(view, branding, uiCustomization)
            }
            is Chip -> {
                applyChipStyling(view, branding, uiCustomization)
            }
        }
    }
    
    /**
     * Apply card styling based on configuration
     */
    private fun applyCardStyling(
        card: MaterialCardView, 
        branding: SDKConfig.Branding,
        uiCustomization: SDKConfig.UICustomization
    ) {
        try {
            // Set background color
            branding.surfaceColor?.let { color ->
                card.setCardBackgroundColor(Color.parseColor(color))
            }
            
            // Set corner radius
            val cornerRadius = uiCustomization.bubbleCornerRadius.toFloat()
            card.radius = cornerRadius
            
            // Set elevation
            val elevation = uiCustomization.messageBubbleElevation.toFloat()
            card.cardElevation = elevation
            
        } catch (e: Exception) {
            println("⚠️ Failed to apply card styling: ${e.message}")
        }
    }
    
    /**
     * Apply button styling based on configuration
     */
    private fun applyButtonStyling(
        button: MaterialButton,
        branding: SDKConfig.Branding,
        uiCustomization: SDKConfig.UICustomization
    ) {
        try {
            // Set primary color
            button.setBackgroundColor(Color.parseColor(branding.primaryColor))
            
            // Set text color
            branding.textColor?.let { color ->
                button.setTextColor(Color.parseColor(color))
            }
            
            // Set corner radius
            val cornerRadius = uiCustomization.bubbleCornerRadius.toFloat()
            button.cornerRadius = cornerRadius.toInt()
            
        } catch (e: Exception) {
            println("⚠️ Failed to apply button styling: ${e.message}")
        }
    }
    
    /**
     * Apply text styling based on configuration
     */
    private fun applyTextStyling(
        textView: TextView,
        branding: SDKConfig.Branding,
        uiCustomization: SDKConfig.UICustomization
    ) {
        try {
            // Set text color
            branding.textColor?.let { color ->
                textView.setTextColor(Color.parseColor(color))
            }
            
            // Set text size based on type
            val textSize = when (textView.tag) {
                "message" -> uiCustomization.messageTextSize.toFloat()
                "timestamp" -> uiCustomization.timestampTextSize.toFloat()
                "sender_name" -> uiCustomization.senderNameTextSize.toFloat()
                "title" -> uiCustomization.titleTextSize.toFloat()
                else -> uiCustomization.messageTextSize.toFloat()
            }
            textView.textSize = textSize
            
        } catch (e: Exception) {
            println("⚠️ Failed to apply text styling: ${e.message}")
        }
    }
    
    /**
     * Apply input field styling based on configuration
     */
    private fun applyInputStyling(
        inputLayout: TextInputLayout,
        branding: SDKConfig.Branding,
        uiCustomization: SDKConfig.UICustomization
    ) {
        try {
            // Set hint text
            inputLayout.hint = uiCustomization.inputPlaceholderText
            
            // Set colors
            inputLayout.setBoxStrokeColor(Color.parseColor(branding.primaryColor))
            inputLayout.hintTextColor = branding.textColor?.let { 
                android.content.res.ColorStateList.valueOf(Color.parseColor(it)) 
            }
            
            // Set corner radius
            val cornerRadius = when (uiCustomization.inputFieldStyle) {
                "rounded" -> uiCustomization.bubbleCornerRadius.toFloat()
                "square" -> 0f
                else -> uiCustomization.bubbleCornerRadius.toFloat()
            }
            inputLayout.boxCornerRadiusTopStart = cornerRadius
            inputLayout.boxCornerRadiusTopEnd = cornerRadius
            inputLayout.boxCornerRadiusBottomStart = cornerRadius
            inputLayout.boxCornerRadiusBottomEnd = cornerRadius
            
        } catch (e: Exception) {
            println("⚠️ Failed to apply input styling: ${e.message}")
        }
    }
    
    /**
     * Apply chip styling based on configuration
     */
    private fun applyChipStyling(
        chip: Chip,
        branding: SDKConfig.Branding,
        uiCustomization: SDKConfig.UICustomization
    ) {
        try {
            // Set colors
            chip.setChipBackgroundColor(
                android.content.res.ColorStateList.valueOf(
                    Color.parseColor(branding.accentColor ?: branding.primaryColor)
                )
            )
            
            branding.textColor?.let { color ->
                chip.setTextColor(Color.parseColor(color))
            }
            
            // Set corner radius
            chip.chipCornerRadius = uiCustomization.bubbleCornerRadius.toFloat()
            
        } catch (e: Exception) {
            println("⚠️ Failed to apply chip styling: ${e.message}")
        }
    }
    
    /**
     * Create a message bubble drawable with custom styling
     */
    fun createMessageBubble(
        config: SDKConfig,
        isFromUser: Boolean
    ): GradientDrawable {
        val drawable = GradientDrawable()
        
        try {
            // Set shape
            drawable.shape = GradientDrawable.RECTANGLE
            
            // Set corner radius
            val cornerRadius = config.uiCustomization.bubbleCornerRadius.toFloat()
            drawable.cornerRadius = cornerRadius
            
            // Set color based on sender
            val color = if (isFromUser) {
                Color.parseColor(config.branding.primaryColor)
            } else {
                Color.parseColor(config.branding.surfaceColor ?: "#f8fafc")
            }
            drawable.setColor(color)
            
            // Set elevation (stroke for shadow effect)
            if (config.uiCustomization.messageBubbleElevation > 0) {
                drawable.setStroke(
                    1, 
                    Color.parseColor("#e5e7eb") // Light gray border
                )
            }
            
        } catch (e: Exception) {
            println("⚠️ Failed to create message bubble: ${e.message}")
            // Fallback to default styling
            drawable.setColor(Color.parseColor("#f8fafc"))
            drawable.cornerRadius = 16f
        }
        
        return drawable
    }
    
    /**
     * Apply spacing and padding based on configuration
     */
    fun applySpacing(view: View, config: SDKConfig) {
        try {
            val uiCustomization = config.uiCustomization
            
            // Apply padding based on view type
            val padding = when (view.tag) {
                "screen" -> uiCustomization.screenPadding
                "message" -> uiCustomization.messagePadding
                "list_item" -> uiCustomization.listItemPadding
                else -> uiCustomization.screenPadding
            }
            
            view.setPadding(padding, padding, padding, padding)
            
        } catch (e: Exception) {
            println("⚠️ Failed to apply spacing: ${e.message}")
        }
    }
    
    /**
     * Get custom text based on configuration
     */
    fun getCustomText(config: SDKConfig, textKey: String): String {
        return try {
            when (textKey) {
                "resolve_button" -> config.uiCustomization.resolveButtonText
                "reopen_button" -> config.uiCustomization.reopenButtonText
                "send_button" -> config.uiCustomization.sendButtonText
                "retry_button" -> config.uiCustomization.retryButtonText
                "loading" -> config.uiCustomization.loadingText
                "connecting" -> config.uiCustomization.connectingText
                "offline" -> config.uiCustomization.offlineText
                "empty_inbox_title" -> config.uiCustomization.emptyInboxTitle
                "empty_inbox_message" -> config.uiCustomization.emptyInboxMessage
                "empty_chat_title" -> config.uiCustomization.emptyChatTitle
                "empty_chat_message" -> config.uiCustomization.emptyChatMessage
                "input_placeholder" -> config.uiCustomization.inputPlaceholderText
                else -> textKey // Return original if not found
            }
        } catch (e: Exception) {
            println("⚠️ Failed to get custom text for $textKey: ${e.message}")
            textKey // Return original on error
        }
    }
    
    /**
     * Check if a feature is enabled
     */
    fun isFeatureEnabled(config: SDKConfig, feature: String): Boolean {
        return try {
            when (feature) {
                "push_notifications" -> config.features.pushNotifications
                "file_upload" -> config.features.fileUpload
                "voice_messages" -> config.features.voiceMessages
                "video_messages" -> config.features.videoMessages
                "typing_indicators" -> config.features.typingIndicators
                "read_receipts" -> config.features.readReceipts
                "conversation_search" -> config.features.conversationSearch
                "emoji_reactions" -> config.features.emojiReactions
                "message_forwarding" -> config.features.messageForwarding
                "conversation_export" -> config.features.conversationExport
                "dark_mode" -> config.features.darkMode
                "auto_translate" -> config.features.autoTranslate
                else -> false
            }
        } catch (e: Exception) {
            println("⚠️ Failed to check feature $feature: ${e.message}")
            false
        }
    }
}