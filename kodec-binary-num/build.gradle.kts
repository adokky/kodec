plugins {
    id("io.github.adokky.quick-mpp")
    id("io.github.adokky.quick-publish")
}

version = "1.1"

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