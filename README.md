# NeuralNodes Inbox SDK for Android

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![GitHub Release](https://img.shields.io/github/v/release/neuralnodes-sdk/neuralnodes-inbox-android)](https://github.com/neuralnodes-sdk/neuralnodes-inbox-android/releases)

A powerful, flexible Android SDK for integrating customer support inbox functionality into your app. Choose from plug-and-play UI components or build completely custom interfaces with our headless API.

## Features

- **Multi-channel Support** - WhatsApp, Email, SMS, Web Chat
- **Real-time Messaging** - Instant message delivery with Ably and Pusher
- **Live Chat Escalations** - Handle escalated conversations
- **Push Notifications** - FCM integration for message alerts
- **Status Management** - Active, Pending, Resolved, Closed workflows
- **Message Pagination** - Efficient loading of conversation history
- **Optimistic Updates** - Instant UI feedback
- **Flexible Integration** - Use pre-built UI or build your own

## Requirements

- Android 7.0+ (API level 24)
- Kotlin 1.9+
- AndroidX
- Java 17+

## Installation

### Prerequisites

You'll need a GitHub Personal Access Token (PAT) to access GitHub Packages:

1. Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token with `read:packages` scope
3. Save the token securely

### Gradle (Kotlin DSL)

Add GitHub Packages repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // GitHub Packages
        maven {
            url = uri("https://maven.pkg.github.com/neuralnodes-sdk/neuralnodes-inbox-android")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Add your credentials to `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

Add the SDK to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.neuralnodes.inbox:neuralnodes-inbox-android:2.2.0")
}
```

### Gradle (Groovy)

Add GitHub Packages repository to your `settings.gradle`:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // GitHub Packages
        maven {
            url = uri("https://maven.pkg.github.com/neuralnodes-sdk/neuralnodes-inbox-android")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Add your credentials to `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

Add the SDK to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'com.neuralnodes.inbox:neuralnodes-inbox-android:2.2.0'
}
```

### Alternative: Direct AAR Download

If you prefer not to use GitHub Packages, you can download the AAR directly:

1. Go to [Releases](https://github.com/neuralnodes-sdk/neuralnodes-inbox-android/releases)
2. Download `neuralnodes-inbox-android-{version}.aar`
3. Place it in your `app/libs/` folder
4. Add to your `build.gradle`:

```gradle
dependencies {
    implementation files('libs/neuralnodes-inbox-android-2.2.0.aar')
    
    // Add required dependencies
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'io.ably:ably-android:1.2.43'
    implementation 'com.pusher:pusher-java-client:2.4.4'
    
    // Jetpack Compose (if using Component mode)
    def composeBom = platform('androidx.compose:compose-bom:2024.01.00')
    implementation composeBom
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.activity:activity-compose:1.8.2'
}
```

## Quick Start

### 1. Initialize the SDK

```kotlin
import com.neuralnodes.inbox.NeuralNodesInbox
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var sdk: NeuralNodesInbox
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sdk = NeuralNodesInbox.getInstance("your-api-key")
        
        lifecycleScope.launch {
            sdk.initialize().onSuccess { config ->
                println("SDK initialized successfully")
            }.onFailure { error ->
                println("Initialization failed: ${error.message}")
            }
        }
    }
}
```

### 2. Choose Your Integration Level

The SDK offers three integration approaches, from easiest to most customizable:

---

## Integration Options

### Option 1: Plug & Play (Fastest) ⚡

**Best for:** Quick integration, standard UI requirements

Get a complete inbox interface with just one line of code:

```kotlin
import com.neuralnodes.inbox.NeuralNodesInbox

class MainActivity : AppCompatActivity() {
    private lateinit var sdk: NeuralNodesInbox
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sdk = NeuralNodesInbox.getInstance("your-api-key")
        
        // Initialize SDK
        lifecycleScope.launch {
            sdk.initialize()
        }
        
        // Show full inbox UI
        findViewById<Button>(R.id.openInboxButton).setOnClickListener {
            sdk.showInbox(this)
        }
        
        // Show live chat UI
        findViewById<Button>(R.id.openChatButton).setOnClickListener {
            sdk.showLiveChat(this)
        }
    }
}
```

**What you get:**
- Complete inbox and live chat UI
- All features working out of the box
- Professional, tested interface
- Zero UI code required

**Time to integrate:** 5 minutes

---

### Option 2: Component Integration (Flexible) 🎨

**Best for:** Custom app structure, branded experience

Use individual SDK views within your own navigation:

#### Jetpack Compose

```kotlin
import androidx.compose.runtime.Composable
import com.neuralnodes.inbox.compose.*

@Composable
fun MyApp() {
    val sdk = NeuralNodesInbox.getInstance()
    
    // Use individual composable views
    Column {
        Text("My Custom Header")
        
        // Embed inbox view
        InboxView(
            sdk = sdk,
            onConversationClick = { conversation ->
                // Navigate to conversation detail
            }
        )
    }
}
```

#### Available Composable Views

```kotlin
// Main Views
InboxView(sdk = sdk)                                    // Conversation list with filters
LiveChatView(escalationId = id, sdk = sdk)             // Live chat interface
InboxTabView(sdk = sdk)                                 // Complete tab bar UI

// Utility Views
EmptyStateView(icon = "📭", title = "No Messages")     // Empty state
ErrorView(message = "Error", onRetry = {})             // Error state
LoadingView()                                           // Loading indicator
SearchBar(searchText = text, onSearch = {})            // Search bar
ConnectionBanner()                                      // Connection status
```

#### XML + Fragments

```kotlin
import com.neuralnodes.inbox.fragments.InboxFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val sdk = NeuralNodesInbox.getInstance()
        
        // Embed inbox fragment
        val fragment = InboxFragment.newInstance(sdk)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
```

**Layout XML:**
```xml
<FrameLayout
    android:id="@+id/container"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

#### Example: Bottom Navigation

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var sdk: NeuralNodesInbox
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        sdk = NeuralNodesInbox.getInstance("your-api-key")
        
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showHomeFragment()
                R.id.nav_inbox -> showInboxFragment()  // SDK fragment
                R.id.nav_profile -> showProfileFragment()
            }
            true
        }
    }
    
    private fun showInboxFragment() {
        val fragment = InboxFragment.newInstance(sdk)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
```

**Time to integrate:** 30 minutes

---

### Option 3: Headless API (Full Control) 🔧

**Best for:** Completely custom UI, unique design requirements

Build your own interface using SDK data and APIs:

#### Get API Clients

```kotlin
val apiClient = sdk.getAPIClient()
val liveChatClient = sdk.getLiveChatClient()
val realtimeClient = sdk.getRealtimeClient()
val pusherClient = sdk.getPusherClient()
val searchService = sdk.getSearchService()
```

#### Fetch Conversations

```kotlin
// Get conversations with filters
val conversations = apiClient.getConversations(
    channel = "whatsapp",
    status = "active",
    limit = 20,
    offset = 0
)

// Get specific conversation
val conversation = apiClient.getConversation(conversationId)

// Get messages
val messages = apiClient.getConversationMessages(
    conversationId = conversationId,
    limit = 50,
    offset = 0
)
```

#### Send Messages

```kotlin
val message = apiClient.sendMessage(
    conversationId = conversationId,
    text = "Hello, how can I help?"
)
```

#### Update Status

```kotlin
apiClient.updateConversationStatus(
    conversationId = conversationId,
    status = "resolved"
)

apiClient.markAsRead(conversationId)
```

#### Real-time Updates

```kotlin
// Subscribe to conversation updates
lifecycleScope.launch {
    realtimeClient.subscribeToConversation(conversationId).collect { message ->
        // Update your UI with new message
        println("New message: ${message.messageText}")
    }
}

// Unsubscribe when done
realtimeClient.unsubscribe(conversationId)
```

#### Live Chat

```kotlin
// Get escalations
val escalations = liveChatClient.getEscalations(limit = 50)

// Get escalation messages
val messages = liveChatClient.getEscalationMessages(
    escalationId = escalationId,
    limit = 50,
    offset = 0
)

// Send message
val message = liveChatClient.sendEscalationMessage(
    escalationId = escalationId,
    text = "Message text"
)

// Subscribe to live chat updates
pusherClient?.subscribeToEscalation(
    escalationId,
    onMessage = { message ->
        // Handle new message
    },
    onTyping = { isTyping ->
        // Handle typing indicator
    }
)
```

#### Example: Custom ViewModel

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralnodes.inbox.NeuralNodesInbox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CustomInboxViewModel : ViewModel() {
    private val sdk = NeuralNodesInbox.getInstance()
    private val apiClient = sdk.getAPIClient()
    
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = apiClient.getConversations()
                _conversations.value = result
            } catch (e: Exception) {
                println("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun sendMessage(conversationId: String, text: String) {
        viewModelScope.launch {
            try {
                val message = apiClient.sendMessage(conversationId, text)
                // Update your UI
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }
}
```

**Time to integrate:** 2-4 hours

---

## Push Notifications

### 1. Add Firebase

Add Firebase to your project: https://firebase.google.com/docs/android/setup

Add dependencies to your `build.gradle`:

```gradle
dependencies {
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-messaging'
}
```

### 2. Register Device Token

```kotlin
import com.google.firebase.messaging.FirebaseMessaging

FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        lifecycleScope.launch {
            sdk.registerForPushNotifications(token)
        }
    }
}
```

### 3. Handle Notifications

```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val sdk = NeuralNodesInbox.getInstance()
        val (conversationId, escalationId) = sdk.handlePushNotification(
            remoteMessage.data
        )
        
        // Navigate to conversation if needed
        conversationId?.let {
            // Open conversation
        }
    }
}
```

---

## API Reference

### Core SDK

```kotlin
// Initialize
val sdk = NeuralNodesInbox.getInstance(apiKey: String)
suspend fun initialize(): Result<SDKConfig>

// Properties
sdk.isInitialized: Boolean
NeuralNodesInbox.VERSION: String
NeuralNodesInbox.FULL_VERSION: String

// Get Clients
sdk.getAPIClient(): APIClient
sdk.getLiveChatClient(): LiveChatClient
sdk.getRealtimeClient(): RealtimeClient
sdk.getPusherClient(): PusherClient?
sdk.getSearchService(): SearchService

// Push Notifications
suspend fun registerForPushNotifications(token: String)
fun handlePushNotification(data: Map<String, String>): Pair<String?, String?>

// Cleanup
fun disconnect()
```

### APIClient

```kotlin
// Configuration
suspend fun getConfig(): SDKConfig

// Conversations
suspend fun getConversations(
    channel: String? = null,
    status: String? = null,
    limit: Int = 50,
    offset: Int = 0
): List<Conversation>

suspend fun getConversation(id: String): Conversation

suspend fun getConversationMessages(
    conversationId: String,
    limit: Int = 50,
    offset: Int = 0
): List<Message>

suspend fun sendMessage(
    conversationId: String,
    text: String
): Message

suspend fun updateConversationStatus(
    conversationId: String,
    status: String
)

suspend fun markAsRead(conversationId: String)
```

### LiveChatClient

```kotlin
suspend fun getEscalations(
    status: String? = null,
    limit: Int = 50,
    offset: Int = 0
): List<Escalation>

suspend fun getEscalationMessages(
    escalationId: String,
    limit: Int = 100,
    offset: Int = 0
): List<ChatMessage>

suspend fun sendEscalationMessage(
    escalationId: String,
    text: String
): ChatMessage

suspend fun resolveEscalation(
    escalationId: String,
    notes: String? = null
)

suspend fun endEscalation(
    escalationId: String,
    reason: String? = null
)

suspend fun transferEscalation(
    escalationId: String,
    toAgentId: String
)
```

### RealtimeClient

```kotlin
fun subscribeToConversation(conversationId: String): Flow<Message>
fun unsubscribe(conversationId: String)
```

### PusherClient

```kotlin
fun subscribeToEscalation(
    escalationId: String,
    onMessage: (ChatMessage) -> Unit,
    onTyping: (Boolean) -> Unit
)

fun unsubscribe(escalationId: String)
```

---

## Models

### Conversation

```kotlin
data class Conversation(
    val id: String,
    val channel: String,
    val status: String,
    val contactName: String?,
    val contactPhone: String?,
    val contactEmail: String?,
    val lastMessage: String?,
    val lastMessageAt: Date?,
    val unreadCount: Int,
    val createdAt: Date,
    val updatedAt: Date
)
```

### Message

```kotlin
data class Message(
    val id: String,
    val conversationId: String,
    val messageType: String,
    val messageText: String,
    val senderType: String,
    val senderName: String?,
    val senderId: String?,
    val isRead: Boolean,
    val readAt: Date?,
    val createdAt: Date
)
```

### Escalation

```kotlin
data class Escalation(
    val id: String,
    val conversationId: String,
    val status: String,
    val reason: String?,
    val customerName: String?,
    val lastMessageAt: Date?,
    val createdAt: Date
)
```

### ChatMessage

```kotlin
data class ChatMessage(
    val id: String,
    val escalationId: String,
    val messageType: String,
    val messageText: String,
    val senderType: String,
    val senderName: String?,
    val createdAt: Date
)
```

---

## Comparison Table

| Feature | Plug & Play | Component | Headless API |
|---------|-------------|-----------|--------------|
| **Setup Time** | 5 minutes | 30 minutes | 2-4 hours |
| **UI Control** | Low | Medium | Full |
| **Customization** | Theme only | Layout & Navigation | Everything |
| **Code Required** | 1 line | 10-50 lines | 100+ lines |
| **Best For** | Quick setup | Branded apps | Unique designs |
| **Maintenance** | SDK handles | Shared | You handle |

---

## Best Practices

### 1. Initialize Early

Initialize the SDK in your Application class or main activity:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val sdk = NeuralNodesInbox.getInstance("your-api-key")
        lifecycleScope.launch {
            sdk.initialize()
        }
    }
}
```

### 2. Handle Errors Gracefully

```kotlin
lifecycleScope.launch {
    try {
        val conversations = apiClient.getConversations()
    } catch (e: Exception) {
        // Show user-friendly error message
        showError("Unable to load conversations. Please try again.")
    }
}
```

### 3. Cleanup on Logout

```kotlin
fun logout() {
    sdk.disconnect()
    // Clear user data
}
```

### 4. Use Real-time Subscriptions Wisely

```kotlin
override fun onResume() {
    super.onResume()
    // Subscribe when view is visible
    lifecycleScope.launch {
        realtimeClient.subscribeToConversation(conversationId).collect { message ->
            // Handle message
        }
    }
}

override fun onPause() {
    super.onPause()
    // Unsubscribe when view is hidden
    realtimeClient.unsubscribe(conversationId)
}
```

---

## Troubleshooting

### SDK Not Initializing

**Problem:** `initialize()` fails or times out

**Solutions:**
- Verify your API key is correct
- Check internet connection
- Check Logcat for error messages: `adb logcat | grep NeuralNodes`
- Ensure `INTERNET` permission is granted

### Build Errors

**Problem:** Gradle sync fails or build errors

**Solutions:**
- Ensure GitHub Packages repository is added correctly
- Check minimum SDK version is 24+
- Verify your GitHub token has `read:packages` scope
- Clean and rebuild: `./gradlew clean build`
- Invalidate caches: File → Invalidate Caches / Restart

### Push Notifications Not Working

**Problem:** Not receiving push notifications

**Solutions:**
- Verify Firebase is configured correctly
- Check `google-services.json` is in `app/` folder
- Ensure FCM server key is configured in NeuralNodes dashboard
- Check notification permissions are granted
- Test with Firebase Console test message

---

## Support

- **Documentation:** [GitHub README](https://github.com/neuralnodes-sdk/neuralnodes-inbox-android)
- **Email:** support@neuralnodes.com
- **Issues:** [GitHub Issues](https://github.com/neuralnodes-sdk/neuralnodes-inbox-android/issues)

---

## License

Copyright © 2024 NeuralNodes. All rights reserved.

This SDK is proprietary software. Unauthorized copying, distribution, or modification is prohibited.

---

**NeuralNodes** • [Website](https://neuralnodes.space) • [Dashboard](https://client.neuralnodes.space)
