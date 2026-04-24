# NeuralNodes Inbox SDK for Android

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![JitPack](https://jitpack.io/v/neuralnodes/neuralnodes-inbox-android.svg)](https://jitpack.io/#neuralnodes/neuralnodes-inbox-android)

The official Android SDK for NeuralNodes Inbox - a unified messaging platform that enables Customer Support Representatives (CSRs) to manage conversations from web chat, WhatsApp, Telegram, and email in a single mobile application.

## Features

- **Unified Inbox**: Manage all conversations from one place
- **Multi-Channel Support**: Web Chat, WhatsApp, Telegram, Email
- **Real-Time Messaging**: Instant message delivery via Ably
- **Push Notifications**: FCM-powered notifications for new messages
- **File Attachments**: Send and receive images, documents, and files
- **Customizable Branding**: Match your brand colors and logo
- **Offline Support**: Queue messages when offline, sync when online
- **Conversation Management**: Mark as read, resolve, filter by status
- **Material Design**: Modern, native Android UI components
- **Secure Communication**: End-to-end encrypted messaging

## Requirements

- **Minimum SDK**: Android 7.0 (API level 24)
- **Target SDK**: Android 14 (API level 34)
- **Language**: Kotlin 1.9+
- **Dependencies**: AndroidX, Kotlin Coroutines

## Installation

### Step 1: Add JitPack Repository

Add JitPack to your root `settings.gradle` or `settings.gradle.kts`:

**Groovy (settings.gradle):**
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**Kotlin (settings.gradle.kts):**
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2: Add SDK Dependency

Add the SDK to your app's `build.gradle` or `build.gradle.kts`:

**Groovy (build.gradle):**
```gradle
dependencies {
    implementation 'com.github.neuralnodes:neuralnodes-inbox-android:1.0.0'
}
```

**Kotlin (build.gradle.kts):**
```kotlin
dependencies {
    implementation("com.github.neuralnodes:neuralnodes-inbox-android:1.0.0")
}
```

### Step 3: Sync Project

Click **"Sync Now"** in Android Studio to download the SDK.

## Quick Start

### 1. Initialize the SDK

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.neuralnodes.inbox.NeuralNodesInbox
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var inbox: NeuralNodesInbox
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize SDK with your API key
        inbox = NeuralNodesInbox("your-api-key-here")
        
        // Load configuration
        lifecycleScope.launch {
            inbox.initialize().onSuccess { config ->
                println("SDK initialized successfully")
                println("Features: ${config.features}")
            }.onFailure { error ->
                println("Initialization failed: ${error.message}")
            }
        }
    }
}
```

### 2. Display the Inbox

```kotlin
import android.widget.Button

class MainActivity : AppCompatActivity() {
    // ... initialization code ...
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        findViewById<Button>(R.id.openInboxButton).setOnClickListener {
            // Show the inbox UI
            inbox.showInbox(this)
        }
    }
}
```

### 3. Setup Push Notifications (Optional)

#### Add Firebase to Your Project

1. Add Firebase to your app: https://firebase.google.com/docs/android/setup
2. Download `google-services.json` and place it in your `app/` folder

#### Configure FCM

**Add dependencies:**
```gradle
dependencies {
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-messaging-ktx'
}
```

**Register device token:**
```kotlin
import com.google.firebase.messaging.FirebaseMessaging

FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        lifecycleScope.launch {
            inbox.registerForPushNotifications(token)
        }
    }
}
```

## Configuration

All configuration is managed through your NeuralNodes Dashboard:

1. Login to your dashboard at https://client.neuralnodes.space
2. Navigate to Settings → Mobile SDK
3. Configure the following options:
   - Enable/disable SDK
   - Brand colors (primary, secondary, accent)
   - Push notifications
   - File uploads
   - Voice messages
   - Typing indicators
   - Read receipts

Configuration changes apply instantly to all devices without requiring app updates.

## API Reference

### NeuralNodesInbox

Main SDK class for managing conversations.

```kotlin
class NeuralNodesInbox(apiKey: String)
```

#### Methods

| Method | Description | Return Type |
|--------|-------------|-------------|
| `suspend fun initialize()` | Load SDK configuration from server | `Result<SDKConfig>` |
| `fun showInbox(context: Context)` | Display the inbox UI | `Unit` |
| `suspend fun registerForPushNotifications(token: String)` | Register FCM device token | `Result<Unit>` |
| `fun handlePushNotification(data: Map<String, String>)` | Handle incoming push notification | `String?` |

### APIClient

Low-level HTTP client for custom implementations.

```kotlin
class APIClient(apiKey: String, baseUrl: String = "https://api.neuralnodes.space")
```

#### Methods

| Method | Description | Return Type |
|--------|-------------|-------------|
| `suspend fun getConversations(...)` | Fetch conversations with filters | `List<Conversation>` |
| `suspend fun getMessages(conversationId: String)` | Get messages for a conversation | `List<Message>` |
| `suspend fun sendMessage(conversationId: String, text: String)` | Send a text message | `Message` |
| `suspend fun markAsRead(conversationId: String)` | Mark conversation as read | `Unit` |
| `suspend fun updateStatus(conversationId: String, status: String)` | Update conversation status | `Unit` |

### Models

#### Conversation
```kotlin
data class Conversation(
    val id: String,
    val channel: String,              // "web", "whatsapp", "telegram", "email"
    val contactName: String?,
    val contactPhone: String?,
    val lastMessage: String?,
    val unreadCount: Int,
    val status: String,               // "active", "pending", "resolved"
    val createdAt: Date,
    val updatedAt: Date
)
```

#### Message
```kotlin
data class Message(
    val id: String,
    val conversationId: String,
    val messageText: String,
    val senderType: String,           // "user", "agent", "bot"
    val senderName: String?,
    val attachmentUrl: String?,
    val createdAt: Date
)
```

## Advanced Usage

### Custom UI Implementation

Build your own UI using the SDK's API client:

```kotlin
import com.neuralnodes.inbox.network.APIClient
import kotlinx.coroutines.flow.MutableStateFlow

class CustomInboxViewModel {
    private val apiClient = APIClient("your-api-key")
    val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    
    suspend fun loadConversations(channel: String? = null, status: String? = null) {
        try {
            val result = apiClient.getConversations(
                channel = channel,
                status = status,
                limit = 50,
                offset = 0
            )
            conversations.value = result
        } catch (e: Exception) {
            println("Error loading conversations: ${e.message}")
        }
    }
    
    suspend fun sendMessage(conversationId: String, text: String) {
        try {
            val message = apiClient.sendMessage(conversationId, text)
            println("Message sent: ${message.id}")
        } catch (e: Exception) {
            println("Error sending message: ${e.message}")
        }
    }
}
```

### Real-Time Updates

Subscribe to live conversation updates:

```kotlin
import com.neuralnodes.inbox.network.RealtimeClient
import kotlinx.coroutines.flow.collect

val realtimeClient = RealtimeClient()
realtimeClient.connect(config.ablyKey!!)

lifecycleScope.launch {
    realtimeClient.subscribeToConversation(conversationId).collect { message ->
        println("New message: ${message.messageText}")
        // Update your UI
    }
}
```

## Permissions

The SDK automatically adds these permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

No additional configuration needed!

## ProGuard / R8

The SDK includes its own ProGuard rules. No additional configuration required.

If you encounter issues, add these rules to your `proguard-rules.pro`:

```proguard
-keep class com.neuralnodes.inbox.** { *; }
-keep class io.ably.** { *; }
```

## Troubleshooting

### SDK Not Initializing

**Problem:** `initialize()` fails or times out

**Solutions:**
- Verify your API key is correct
- Check internet connection
- Check Logcat for error messages: `adb logcat | grep NeuralNodes`
- Ensure `INTERNET` permission is granted

### Push Notifications Not Working

**Problem:** Not receiving push notifications

**Solutions:**
- Verify Firebase is configured correctly
- Check `google-services.json` is in `app/` folder
- Ensure FCM server key is configured in NeuralNodes dashboard
- Check notification permissions are granted
- Test with Firebase Console test message

### Real-Time Updates Not Working

**Problem:** Messages don't appear instantly

**Solutions:**
- Check Ably key is valid in SDK config
- Verify internet connection is stable
- Check Logcat for Ably connection errors
- Ensure app is not in battery optimization mode

### Build Errors

**Problem:** Gradle sync fails or build errors

**Solutions:**
- Ensure JitPack repository is added
- Check minimum SDK version is 24+
- Clean and rebuild: `./gradlew clean build`
- Invalidate caches: File → Invalidate Caches / Restart

## Documentation

- **Dashboard**: https://client.neuralnodes.space
<!-- - **API Documentation**: https://docs.neuralnodes.space -->
- **Support**: support@neuralnodes.space

## Support

For technical support and assistance:

- **Email**: support@neuralnodes.space
- **Issues**: [GitHub Issues](https://github.com/neuralnodes/neuralnodes-inbox-android/issues)
<!-- - **Documentation**: https://docs.neuralnodes.space -->
- **Live Chat**: Available in your dashboard

## License

This SDK is released under the MIT License. See [LICENSE](LICENSE) for details.

```
MIT License

Copyright (c) 2024 NeuralNodes

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

## Changelog

### Version 1.0.0 (2024-04-23)

**Initial Release**

- Real-time messaging
- Push notifications
- Multi-channel support (Web, WhatsApp, Telegram, Email)
- Material Design UI
- Conversation management (read, resolve, filter)
- File attachments support
- Offline message queue
- Customizable branding
- Typing indicators
- Read receipts

---

**NeuralNodes**

[Website](https://neuralnodes.space) • [Dashboard](https://client.neuralnodes.space)

 <!-- • [Documentation](https://docs.neuralnodes.space) -->
