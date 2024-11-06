plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 35

    defaultConfig {
        minSdk = 25
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

//    libraryVariants.configureEach { variant ->
//        variant.outputs.configureEach { output ->
//            if (outputFile != null && outputFileName.endsWith('.aar')) {
//                outputFileName = "card-sdk-${variant.versionName}.aar"
//            }
//        }
//    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += setOf(
                "/META -INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
            )
        }
    }
    namespace = "tech.ammer.sdk.card"

//    publishing {
//        repositories {
//            maven {
//                name = "Ammer-Tech"
//                url = uri("https://maven.pkg.github.com/Ammer-Tech/publications")
//                credentials {
//                    username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
//                    password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
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
val multidex_version = "2.0.1"

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
//    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("androidx.multidex:multidex:$multidex_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    implementation("commons-codec:commons-codec:1.17.1")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    implementation(files("../libs/NeptuneLiteApi_V3.30.00_20220720.jar"))
}
