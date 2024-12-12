import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

repositories {
    mavenCentral()
    google()
    maven {
        val githubProperties = Properties()
        githubProperties.load(FileInputStream(rootProject.file("github.properties")))

        name = "Ammer-Tech"
        url = uri("https://maven.pkg.github.com/Ammer-Tech/publications")
        credentials {
            username = githubProperties.getProperty("user") ?: System.getenv("user")
            password = githubProperties.getProperty("key") ?: System.getenv("key")
        }
    }
}

android {
    compileSdk = 35

    defaultConfig {
        applicationId = "ammer.sample.apdu"
        minSdk = 25
        versionCode = 2
        versionName = "1.2"
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
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
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")

//    implementation("tech.ammer.sdk.card:apdu:1.0.11")
    implementation(project(":app"))
}