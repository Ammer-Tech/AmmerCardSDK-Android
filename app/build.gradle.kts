plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

version = "1.0.0"
group = "tech.ammer.sdk.card"

android {
    compileSdk = 35
    namespace = "tech.ammer.sdk.card"

    defaultConfig {
        minSdk = 25
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
//        versionName = "1.0.0"
//        versionCode = 1
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

    libraryVariants.all {
        this.outputs.filterIsInstance<com.android.build.gradle.internal.api.BaseVariantOutputImpl>()
            .forEach { output ->
                if (output.outputFileName.endsWith(".aar")) {
                    output.outputFileName = "ammer_card_sdk_${version}.aar"
                }
            }
    }


//    publishing {
//        repositories {
//            maven {
//                name = "Ammer-Tech"
//                url = uri("https://maven.pkg.github.com/Ammer-Tech/publications")
//                credentials {
//                    username = (project.findProperty("gpr.user") ?: System.getenv("USERNAME")).toString()
//                    password = (project.findProperty("gpr.key") ?: System.getenv("TOKEN")).toString()
//                }
//            }
//        }
//        publications {
//            gpr(MavenPublication) {
//                from(components.java)
//            }
//        }
//    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
}
