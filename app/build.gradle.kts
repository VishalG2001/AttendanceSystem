import org.jetbrains.kotlin.storage.CacheResetOnProcessCanceled.enabled

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    kotlin("kapt")
    id("realm-android")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.recycleviewpractice"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.recycleviewpractice"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
    }
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
}

dependencies {
    implementation(libs.jts.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.maps)
    implementation (libs.android.maps.utils)
    implementation(libs.play.services.location)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.ml.vision)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.play.services.vision)

    // face detection
    implementation( libs.aws.android.sdk.rekognition)
    implementation(libs.kotlinx.coroutines.core.v160)
    //bio
    implementation(libs.androidx.biometric)
    //retrofit
    implementation (libs.retrofit)
    //gson
    implementation (libs.converter.gson)
    // QR code generation
    implementation (libs.core)
    implementation (libs.zxing.android.embedded)
    // QR code scanning
    implementation(libs.quickie.bundled)

}