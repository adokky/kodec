plugins {
    alias(libs.plugins.quick.mpp)
    alias(libs.plugins.quick.publish)
}

version = "2.0"

dependencies {
    commonTestImplementation(project(":kodec-testing"))
    commonMainImplementation(libs.karamelUtils.core)
}

mavenPublishing {
    pom {
        description = "Templates for binary encoding/decoding of numbers in plain BE/LE and variable-length format"
        inceptionYear = "2025"
    }
}