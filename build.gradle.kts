// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false

    // Add Hilt plugin
    id("com.google.dagger.hilt.android") version "2.50" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
//        maven {
//            url = uri("https://mvn.0110.be/#/releases")
//        }
        maven("https://jitpack.io")
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath(libs.hilt.android.gradle.plugin)
    }
}