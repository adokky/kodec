plugins {
    id("io.github.adokky.quick-mpp")
    id("io.github.adokky.quick-publish")
}

version = "0.9.2"

dependencies {
    commonMainApi(project(":kodec-strings-common"))
    commonMainApi(project(":kodec-buffers-core"))
    commonTestImplementation(project(":kodec-testing"))
    commonMainImplementation(libs.karamelUtils.core)
}

mavenPublishing {
    pom {
        description = "Convert numbers to string and back (for both stream and random access structures)"
        inceptionYear = "2025"
    }
}