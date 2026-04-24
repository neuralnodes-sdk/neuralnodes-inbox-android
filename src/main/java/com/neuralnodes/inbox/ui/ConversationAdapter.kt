package com.neuralnodes.inbox.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.neuralnodes.inbox.databinding.ItemConversationBinding
import com.neuralnodes.inbox.models.Conversation
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for conversation list
 */
class ConversationAdapter(
    private val onConversationClick: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ViewHolder>(ConversationDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onConversationClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemConversationBinding,
        private val onConversationClick: (Conversation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(conversation: Conversation) {
            binding.apply {
                // Channel icon
                iconText.text = conversation.channelIcon
                
                // Contact name
                nameText.text = conversation.displayName
                
                // Last message
                messageText.text = conversation.lastMessage ?: "No messages"
                
                // Time (shorter format)
                timeText.text = formatTimeShort(conversation.updatedAt)
                
                // Channel badge with color
                channelBadge.text = when (conversation.channel) {
                    "webchat" -> "Web"
                    "whatsapp" -> "WhatsApp"
                    "telegram" -> "Telegram"
                    "email" -> "Email"
                    else -> conversation.channel.uppercase()
                }
                
                channelBadge.setBackgroundResource(when (conversation.channel) {
                    "webchat" -> com.neuralnodes.inbox.R.drawable.channel_badge_webchat
                    "whatsapp" -> com.neuralnodes.inbox.R.drawable.channel_badge_whatsapp
                    "telegram" -> com.neuralnodes.inbox.R.drawable.channel_badge_telegram
                    "email" -> com.neuralnodes.inbox.R.drawable.channel_badge_email
                    else -> com.neuralnodes.inbox.R.drawable.status_badge_active
                })
                
                // Unread badge
                if (conversation.unreadCount > 0) {
                    badgeText.visibility = View.VISIBLE
                    badgeText.text = if (conversation.unreadCount > 9) "9+" else conversation.unreadCount.toString()
                } else {
                    badgeText.visibility = View.GONE
                }
                
                // Click listener
                root.setOnClickListener {
                    onConversationClick(conversation)
                }
            }
        }
        
        private fun formatTimeShort(date: Date): String {
            val now = Date()
            val diff = now.time - date.time
            
            return when {
                diff < 60_000 -> "now"
                diff < 3600_000 -> "${diff / 60_000}m"
                diff < 86400_000 -> "${diff / 3600_000}h"
                diff < 604800_000 -> "${diff / 86400_000}d"
                else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
            }
        }
    }
    
    private class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean {
            return oldItem == newItem
        }
    }
}
