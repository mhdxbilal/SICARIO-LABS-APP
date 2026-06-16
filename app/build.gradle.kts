plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.sicario.labs.mediaplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sicario.labs.mediaplayer"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Offline-first feature flags
        buildConfigField("Boolean", "OFFLINE_MODE_ONLY", "true")
        buildConfigField("Boolean", "ENABLE_LOCAL_CACHE", "true")
        buildConfigField("Boolean", "ENABLE_ANALYTICS", "false")
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
            storeFile = file(keystorePath)
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = "upload"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
        create("debugConfig") {
            val localKeystore = rootProject.file("debug.keystore")
            if (localKeystore.exists()) {
                storeFile = localKeystore
            } else {
                storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            }
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isCrunchPngs = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("Boolean", "OFFLINE_MODE_ONLY", "true")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debugConfig")
            buildConfigField("Boolean", "OFFLINE_MODE_ONLY", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.
  
  // JSON
  implementation(libs.moshi.kotlin)
  "ksp"(libs.moshi.kotlin.codegen)
  
  // Image Loading
  implementation(libs.coil.compose)
  
  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)
  
  // Camera (for media scanning)
  implementation("androidx.camera:camera-camera2:1.3.1")
  implementation("androidx.camera:camera-core:1.3.1")
  implementation("androidx.camera:camera-lifecycle:1.3.1")
  implementation("androidx.camera:camera-view:1.3.1")
  
  // ML Kit (On-device, no internet required)
  implementation("com.google.mediapipe:tasks-vision:0.20230731")
  
  // Testing
  testImplementation(libs.junit)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  
  // Android Testing
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  
  // Debug Tools
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
