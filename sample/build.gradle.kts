plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sample"
        minSdk = 25
        versionCode = 2
        versionName = "1.2"
        //noinspection EditedTargetSdkVersion
        targetSdk = 35

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += setOf("x86", "x86_64", "armeabi", "armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    namespace = "com.example.sample"
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation(project(":app"))
//    implementation(files("/Users/dmitriikr/AndroidStudioPorjects/AmmerCardSDK-Android/app/build/outputs/aar/app-release.aar"))
}