# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Jetpack Compose rendering and hardware acceleration structures
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.animation.** { *; }

# Enable aggressive static code optimizations for maximum JVM speed
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Keep MediaPipe & Gesture NPU bindings preserved from over-obfuscation
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }

# Optimize Android Graphics and Hardware Acceleration render loops
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Preserve the line number information for crash logs and keep methods active
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod

# Suppress warnings from unused libraries to speed up compile times (de-bloating)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.google.mediapipe.**
-dontwarn retrofit2.**

