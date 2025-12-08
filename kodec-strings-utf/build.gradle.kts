plugins {
    alias(libs.plugins.quick.mpp)
    alias(libs.plugins.quick.publish)
}

version = "0.3.8"

dependencies {
    commonMainApi(project(":kodec-strings-common"))
    commonTestImplementation(project(":kodec-testing"))
    commonMainImplementation(libs.karamelUtils.core)
}

mavenPublishing {
    pom {
        description = "Templates for encoding/decoding UTF-8 strings"
        inceptionYear = "2025"
    }
}