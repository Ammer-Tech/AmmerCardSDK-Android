import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
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

val githubProperties = Properties()
githubProperties.load(FileInputStream(rootProject.file("github.properties")))

publishing {
    publications {
        create<MavenPublication>("gpr") {
            run {
                groupId = "tech.ammer.sdk.card"
                artifactId = "apdu"
                version = "1.0.6"
                artifact("${buildDir}/outputs/aar/app-release.aar")
            }
        }
    }
    repositories {
        maven {
            name = "Ammer-Tech"
            url = uri("https://maven.pkg.github.com/Ammer-Tech/publications")
            credentials {
                username = githubProperties.getProperty("gpr.user") ?: System.getenv("USERNAME")
                password = githubProperties.getProperty("gpr.key") ?: System.getenv("TOKEN")
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
}

task("BuildAndPublish") {
    group = "release"
    dependsOn("build")
    dependsOn("publishGprPublicationToAmmer-TechRepository")
    tasks.findByName("publishGprPublicationToAmmer-TechRepository")
        ?.mustRunAfter("build")
}
