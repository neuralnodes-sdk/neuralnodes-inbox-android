package com.neuralnodes.inbox.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuralnodes.inbox.models.Message
import java.text.SimpleDateFormat
import java.util.*

/**
 * iOS-style Message Bubble
 * Matches iOS SDK exactly: solid blue for agent, gray for user, 20dp corner radius
 */
@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isAgent = message.senderType == "agent"
    val isUser = message.senderType == "user"
    val isSystem = message.senderType == "system"
    
    if (isSystem) {
        // System message - centered
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF3F4F6).copy(alpha = 0.8f)
            ) {
                Text(
                    text = message.messageText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = formatTime(message.createdAt),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280).copy(alpha = 0.8f)
            )
        }
    } else {
        // Regular message - left or right aligned
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = if (isAgent) Arrangement.End else Arrangement.Start
        ) {
            if (isAgent) {
                Spacer(modifier = Modifier.width(60.dp))
            }
            
            Column(
                horizontalAlignment = if (isAgent) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Sender Name (for user messages)
                if (isUser) {
                    Text(
                        text = message.senderName ?: "User",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                // Message Bubble
                Surface(
                    shape = RoundedCornerShape(20.dp), // iOS bubble radius
                    color = if (isAgent) Color(0xFF4A6EE0) else Color(0xFFF3F4F6), // Solid blue for agent
                    shadowElevation = if (isAgent) 8.dp else 2.dp,
                    modifier = Modifier.shadow(
                        elevation = if (isAgent) 8.dp else 2.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = if (isAgent) Color(0xFF667eea).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.08f)
                    )
                ) {
                    Text(
                        text = message.messageText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isAgent) Color.White else Color.Black,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                // Timestamp
                Text(
                    text = formatTime(message.createdAt),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280).copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp, top = 2.dp)
                )
            }
            
            if (isUser) {
                Spacer(modifier = Modifier.width(60.dp))
            }
        }
    }
}

/**
 * Format time for message timestamp
 */
private fun formatTime(date: Date): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(date)
}
