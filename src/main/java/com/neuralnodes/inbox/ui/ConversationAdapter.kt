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
                // Avatar - first letter of name
                avatarText.text = conversation.displayName.firstOrNull()?.uppercase() ?: "A"
                
                // Contact name
                contactName.text = conversation.displayName
                
                // Status - minimal dot + text
                statusText.text = conversation.status.capitalize()
                val statusColor = when (conversation.status.lowercase()) {
                    "active" -> 0xFF10B981.toInt()
                    "resolved" -> 0xFF6B7280.toInt()
                    "closed" -> 0xFF6B7280.toInt()
                    else -> 0xFF6B7280.toInt()
                }
                statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(statusColor)
                statusText.setTextColor(statusColor)
                
                // Last message with sender prefix
                val messagePrefix = if (conversation.lastMessage?.startsWith("Bot:") == true || 
                                      conversation.lastMessage?.startsWith("User:") == true) {
                    ""
                } else {
                    "User: "
                }
                lastMessage.text = messagePrefix + (conversation.lastMessage ?: "No messages")
                
                // Time - iOS style short format
                timestamp.text = formatTimeShort(conversation.updatedAt)
                
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
