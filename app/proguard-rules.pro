# ProGuard Rules for SICARIO LABS Media Player

# General rules
-verbose
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers

# Keep source file names
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve annotations
-keepattributes *Annotation*,InnerClasses

# Keep Kotlin metadata
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes EnclosingMethod

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Room Database
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class com.siciario.labs.** { *; }

# Moshi JSON
-keepclassmembers class ** {
    @com.squareup.moshi.Json <fields>;
}
-keepclasseswithmembers class ** {
    @com.squareup.moshi.Json <methods>;
}

# MediaPipe
-keep class com.google.mediapipe.** { *; }

# ExoPlayer/Media3
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep interface retrofit2.** { *; }
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Coil Image Loading
-keep class coil.** { *; }

# Application classes
-keep class com.siciario.labs.** { *; }
-keepclassmembers class com.siciario.labs.** { *; }

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
