import java.io.FileInputStream
import java.util.Properties

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "Card SDK"
include(":app")
include(":sample")
