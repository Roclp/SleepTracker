plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    // Add Hilt plugin
    id("dagger.hilt.android.plugin")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.SleepTracker"
    compileSdk = 34
//    noCompress = "tflite"
    defaultConfig {
        applicationId = "com.example.SleepTracker"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
//    ndk {
//        abiFilters "armeabi-v7a", "arm64-v8a"
//    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        mlModelBinding = true
    }
}

dependencies {
    // Hilt Dependencies (Latest as of 06/12/2024)
    implementation("com.google.dagger:hilt-android:2.50")
    implementation(libs.protolite.well.known.types)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    ksp("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room Dependencies
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

//    implementation ("com.arthenica:mobile-ffmpeg-full:4.4")
//    implementation("org.tensorflow:tensorflow-lite-task-audio:0.4.4")
//    implementation ("com.googlecode.javacpp:javacpp:1.5.7") // FFmpeg 支持
//    implementation ("com.arthenica:mobile-ffmpeg-audio:4.4.LTS") // 音频处理
//    implementation("com.github.JorenSix:TarsosDSP:2.4")
//    implementation("com.github.Subtitle-Synchronizer:jlibrosa:-SNAPSHOT")

    implementation("com.github.Subtitle-Synchronizer:jlibrosa:master"){
        exclude(group = "org.hamcrest", module = "hamcrest-core")
    }
    implementation ("be.tarsos.dsp:core:2.5")
    implementation ("be.tarsos.dsp:jvm:2.5")
//    implementation ("org.tensorflow:tensorflow-lite:2.12.0")
//    implementation ("org.tensorflow:tensorflow-lite-support:+") // 可选，提供更多工具
    implementation ("com.github.wendykierp:JTransforms:3.1")

    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.1.0")


    implementation("androidx.compose.foundation:foundation:1.5.2")
    implementation (libs.androidx.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.vision.internal.vkp)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 排除冲突的依赖
//    testImplementation(libs.junit) {
//        exclude(group = "org.hamcrest", module = "hamcrest-core")
//    }
}