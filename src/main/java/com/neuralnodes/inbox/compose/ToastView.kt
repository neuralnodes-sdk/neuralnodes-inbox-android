package com.neuralnodes.inbox.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Premium toast notification with Apple-level design
 * Exact match to iOS SDK ToastView
 */
enum class ToastStyle {
    SUCCESS,
    ERROR,
    INFO,
    WARNING;
    
    val color: Color
        get() = when (this) {
            SUCCESS -> Color(0xFF10B981) // Success Green
            ERROR -> Color(0xFFEF4444) // Error Red
            INFO -> Color(0xFF4A6EE0) // Info Blue
            WARNING -> Color(0xFFF59E0B) // Warning Yellow
        }
    
    val icon: ImageVector
        get() = when (this) {
            SUCCESS -> Icons.Default.CheckCircle
            ERROR -> Icons.Default.Cancel
            INFO -> Icons.Default.Info
            WARNING -> Icons.Default.Warning
        }
}

@Composable
fun ToastView(
    message: String,
    style: ToastStyle,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(3000) // Auto dismiss after 3 seconds
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = style.color,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = message,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Toast state holder
 */
class ToastState {
    var message by mutableStateOf("")
        private set
    var style by mutableStateOf(ToastStyle.INFO)
        private set
    var visible by mutableStateOf(false)
        private set
    
    fun show(message: String, style: ToastStyle) {
        this.message = message
        this.style = style
        this.visible = true
    }
    
    fun dismiss() {
        visible = false
    }
}

@Composable
fun rememberToastState(): ToastState {
    return remember { ToastState() }
}
