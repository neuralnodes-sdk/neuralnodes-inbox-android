package com.neuralnodes.inbox

import android.os.Build

/**
 * SDK Version Information
 * Matches iOS SDKVersion implementation
 */
object SDKVersion {
    const val version = "2.2.1"
    
    const val name = "NeuralNodesInbox-Android"
    
    val fullVersion = "$name/$version"
    
    val userAgent: String
        get() {
            val systemVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            val platform = "Android"
            val device = "${Build.MANUFACTURER} ${Build.MODEL}"
            return "$name/$version ($platform; $systemVersion; $device)"
        }
    
    // Build number set by CI/CD or defaults to "dev" for local builds
    var buildNumber: String? = "dev"
    
    val versionWithBuild: String
        get() = buildNumber?.let { "$version+$it" } ?: version
}
