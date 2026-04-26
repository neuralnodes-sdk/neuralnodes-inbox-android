package com.neuralnodes.inbox.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Connection Status Banner Component
 */
@Composable
fun ConnectionBanner(
    status: String = "Reconnecting...",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF59E0B))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⚠️",
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        
        Text(
            text = status,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
