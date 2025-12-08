pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "kodec"

include(":kodec-binary-num")
include(":kodec-buffers-core")
include(":kodec-buffers-data")
include(":kodec-java-io")
include(":kodec-strings-common")
include(":kodec-strings-number")
include(":kodec-strings-stream")
include(":kodec-strings-utf")
include(":kodec-struct")
include(":kodec-testing")

// https://docs.gradle.org/8.11.1/userguide/configuration_cache.html#config_cache:stable
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")