import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

repositories {
    mavenCentral()
    google()
}

android {
    compileSdk = 35
    namespace = "tech.ammer.sdk.card"

    defaultConfig {
        minSdk = 25
        lint.targetSdk = 35
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
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            run {
                groupId = "tech.ammer.sdk.card"
                artifactId = "apdu"
                version = "1.0.11"
                artifact("${layout.buildDirectory.get()}/outputs/aar/app-release.aar")
            }
        }
    }
    repositories {
        val githubProperties = Properties()
        githubProperties.load(FileInputStream(rootProject.file("github.properties")))
        maven {
            name = "Ammer-Tech"
            url = uri("https://maven.pkg.github.com/Ammer-Tech/publications")
            credentials {

                username = githubProperties.getProperty("user").toString()
                password = githubProperties.getProperty("token").toString()
            }
        }
    }
}
task("BuildAndPublish") {
    group = "release"
    dependsOn("build")
    dependsOn("publishGprPublicationToAmmer-TechRepository")
    tasks.findByName("publishGprPublicationToAmmer-TechRepository")?.mustRunAfter("build")
}


dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
}

