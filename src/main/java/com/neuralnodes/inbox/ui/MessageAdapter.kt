package com.neuralnodes.inbox.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.neuralnodes.inbox.databinding.ItemMessageAgentBinding
import com.neuralnodes.inbox.databinding.ItemMessageUserBinding
import com.neuralnodes.inbox.models.Message
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for message list with different view types for user/agent
 */
class MessageAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {
    
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AGENT = 2
        
        private fun formatTime(date: Date): String {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        // Swap: Agent messages on right (blue), User messages on left (gray)
        return if (getItem(position).isFromAgent) VIEW_TYPE_USER else VIEW_TYPE_AGENT
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val binding = ItemMessageUserBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                UserMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemMessageAgentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AgentMessageViewHolder(binding)
            }
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AgentMessageViewHolder -> holder.bind(message)
        }
    }
    
    class UserMessageViewHolder(
        private val binding: ItemMessageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: Message) {
            binding.messageText.text = message.messageText
            binding.timeText.text = formatTime(message.createdAt)
        }
    }
    
    class AgentMessageViewHolder(
        private val binding: ItemMessageAgentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: Message) {
            binding.messageText.text = message.messageText
            binding.timeText.text = formatTime(message.createdAt)
            binding.senderText.text = message.displaySenderName
        }
    }
    
    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
