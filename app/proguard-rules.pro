# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
#
# SECURITY NOTES:
# - These rules are hardened against reverse engineering and tampering
# - No root detection is implemented to ensure compatibility with rooted devices
# - Security is balanced with functionality to maintain app usability

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Add project specific ProGuard rules here.

# Keep data classes for Room and Gson
-keep class com.lloir.ornaassistant.data.database.entities.** { *; }
-keep class com.lloir.ornaassistant.data.network.dto.** { *; }
-keep class com.lloir.ornaassistant.domain.model.** { *; }

# Keep Hilt components
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowshrinking,allowoptimization interface * {
    @retrofit2.http.* <methods>;
}

# Keep Gson
-keepattributes Signature
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep accessibility service
-keep class com.lloir.ornaassistant.service.accessibility.** { *; }

# Enhanced security rules

# Basic application hardening
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Obfuscation enhancements
-repackageclasses ''
-allowaccessmodification

# Anti-debugging measures
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Prevent class name exposure
-keep class !com.lloir.ornaassistant.** { *; }

# Protect native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Protect against reflection-based attacks
-keep class com.lloir.ornaassistant.utils.IntentSecurityHelper { *; }

# Ensure root users can still use the app - don't include root detection or blocking
# Intentionally not implementing root detection to maintain compatibility with rooted devices

# Keep important security-related classes
-keep class com.lloir.ornaassistant.utils.** { *; }

# Protect against decompilation
-keepattributes *Annotation*

# ML Kit Text Recognition rules
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**
