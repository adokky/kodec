plugins {
    id("io.github.adokky.quick-mpp")
    id("io.github.adokky.quick-publish")
}

version = "0.3.6"

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