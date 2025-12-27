plugins {
    alias(libs.plugins.quick.mpp)
    alias(libs.plugins.quick.publish)
}

version = "0.2.2"

dependencies {
    commonMainApi(project(":kodec-buffers-data"))
    commonTestImplementation(project(":kodec-testing"))
    commonMainImplementation(libs.bitvector)
    commonMainImplementation(libs.karamelUtils.core)
}

mavenPublishing {
    pom {
        description = "Type-safe way to encode/decode flat binary structures"
        inceptionYear = "2025"
    }
}