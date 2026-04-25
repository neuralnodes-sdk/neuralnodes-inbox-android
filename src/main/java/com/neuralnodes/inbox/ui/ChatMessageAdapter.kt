package com.neuralnodes.inbox.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.neuralnodes.inbox.R
import com.neuralnodes.inbox.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private var messages = listOf<ChatMessage>()
    
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AGENT = 2
        
        private fun formatTime(date: Date): String {
            val now = Date()
            val diff = now.time - date.time
            
            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                else -> SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()).format(date)
            }
        }
    }
    
    fun submitList(list: List<ChatMessage>) {
        messages = list
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        // Agent (you) = RIGHT/BLUE, Customer = LEFT/GRAY
        return if (messages[position].isFromAgent) VIEW_TYPE_AGENT else VIEW_TYPE_USER
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_AGENT -> {
                // Agent messages on RIGHT with BLUE background
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_agent, parent, false)
                AgentMessageViewHolder(view)
            }
            else -> {
                // User/Customer messages on LEFT with GRAY background
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_user, parent, false)
                UserMessageViewHolder(view)
            }
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AgentMessageViewHolder -> holder.bind(message)
        }
    }
    
    override fun getItemCount() = messages.size
    
    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        
        fun bind(message: ChatMessage) {
            messageText.text = message.messageText
            messageTime.text = formatTime(message.createdAt)
        }
    }
    
    class AgentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        
        fun bind(message: ChatMessage) {
            messageText.text = message.messageText
            messageTime.text = formatTime(message.createdAt)
        }
    }
}
