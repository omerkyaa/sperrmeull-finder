# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# ===== FIREBASE RULES =====
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Google Play Services - Security Exception Fix
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.android.gms.internal.**

# Google Play Services Channels - ManagedChannel Fix
-keep class io.grpc.** { *; }
-keep class com.google.api.client.** { *; }
-dontwarn io.grpc.**
-dontwarn com.google.api.client.**

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.type.** { *; }
-keep class com.google.protobuf.** { *; }

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }

# Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.crashlytics.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }

# Firebase Remote Config
-keep class com.google.firebase.remoteconfig.** { *; }

# Firebase App Check
-keep class com.google.firebase.appcheck.** { *; }

# ===== REVENUECAT RULES =====
-keep class com.revenuecat.purchases.** { *; }
-dontwarn com.revenuecat.purchases.**

# RevenueCat Models
-keep class com.revenuecat.purchases.models.** { *; }
-keep class com.revenuecat.purchases.interfaces.** { *; }

# ===== ML KIT RULES =====
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ML Kit Vision
-keep class com.google.mlkit.vision.** { *; }
-keep class com.google.android.gms.vision.** { *; }

# ML Kit Common
-keep class com.google.mlkit.common.** { *; }

# ===== GLIDE RULES =====
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# Glide Landscapist
-keep class com.github.skydoves.landscapist.** { *; }

# ===== COMPOSE RULES =====
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Compose Runtime
-keep class androidx.compose.runtime.** { *; }

# Compose UI
-keep class androidx.compose.ui.** { *; }

# Compose Material3
-keep class androidx.compose.material3.** { *; }

# ===== HILT RULES =====
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class **_HiltModules*
-keep class **_Provide*Factory*

# Hilt ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ===== ROOM RULES =====
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Room Entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# ===== KOTLIN SERIALIZATION RULES =====
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Serializable classes
-keep @kotlinx.serialization.Serializable class * {
    *** Companion;
}

# ===== RETROFIT & OKHTTP RULES =====
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit interfaces
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ===== NAVIGATION COMPONENT RULES =====
-keep class androidx.navigation.** { *; }

# Navigation SafeArgs
-keepnames class androidx.navigation.fragment.NavHostFragment

# ===== CAMERAX RULES =====
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# CameraX Core
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }

# ===== PAGING 3 RULES =====
-keep class androidx.paging.** { *; }

# Paging DataSource
-keep class * extends androidx.paging.DataSource
-keep class * extends androidx.paging.PagingSource

# ===== WORKMANAGER RULES =====
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class * extends androidx.work.CoroutineWorker

# ===== GOOGLE MAPS RULES =====
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.maps.**

# Maps Compose
-keep class com.google.maps.android.** { *; }

# ===== APP SPECIFIC RULES =====

# Keep data classes for Firestore
-keep class com.omerkaya.sperrmuellfinder.data.model.** { *; }
-keep class com.omerkaya.sperrmuellfinder.domain.model.** { *; }

# Keep Repository implementations
-keep class com.omerkaya.sperrmuellfinder.data.repository.** { *; }

# Keep Use Cases
-keep class com.omerkaya.sperrmuellfinder.domain.usecase.** { *; }

# Keep ViewModels
-keep class com.omerkaya.sperrmuellfinder.ui.** extends androidx.lifecycle.ViewModel { *; }

# Keep Managers
-keep class com.omerkaya.sperrmuellfinder.data.manager.** { *; }

# Keep sealed classes and enums
-keep class com.omerkaya.sperrmuellfinder.** extends kotlin.Enum { *; }
-keep class com.omerkaya.sperrmuellfinder.**$* { *; }

# Keep sealed classes specifically
-keep class com.omerkaya.sperrmuellfinder.**$WhenMappings { *; }
-keepclassmembers class com.omerkaya.sperrmuellfinder.** {
    ** values();
    ** valueOf(java.lang.String);
}

# RESOURCE ID FIX: Keep all resources to prevent resource ID errors
-keep class **.R
-keep class **.R$* { *; }
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ADDITIONAL RESOURCE PROTECTION: Fix for resource ID 0x6a0b000f not found
-keepclassmembers class **.R {
    public static final int *;
}
-keep class **.R$drawable { *; }
-keep class **.R$string { *; }
-keep class **.R$layout { *; }
-keep class **.R$id { *; }
-keep class **.R$color { *; }
-keep class **.R$dimen { *; }
-keep class **.R$style { *; }
-keep class **.R$styleable { *; }

# ===== COROUTINES RULES =====
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Coroutines Flow
-keep class kotlinx.coroutines.flow.** { *; }

# ===== LIFECYCLE RULES =====
-keep class androidx.lifecycle.** { *; }

# Lifecycle ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ===== GENERAL OPTIMIZATION RULES =====

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove custom logger calls
-assumenosideeffects class com.omerkaya.sperrmuellfinder.core.util.Logger {
    public void d(...);
    public void i(...);
    public void w(...);
    public void e(...);
}

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Keep class names for better crash reports in debug builds
-keepnames class com.omerkaya.sperrmuellfinder.**

# Preserve some attributes for reflection and annotations
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ===== KOTLIN ANALYSIS API COMPATIBILITY =====

# Keep Kotlin Analysis API classes to prevent warnings
-keep class org.jetbrains.kotlin.analysis.api.** { *; }
-dontwarn org.jetbrains.kotlin.analysis.api.**

# Suppress Kotlin Analysis API lifetime warnings
-keep class org.jetbrains.kotlin.analysis.api.lifetime.** { *; }
-dontwarn org.jetbrains.kotlin.analysis.api.lifetime.**

# ===== CUSTOM PROGUARD OPTIMIZATIONS =====

# Enable more aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove unused resources
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*