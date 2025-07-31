plugins {
    id("io.github.adokky.quick-mpp")
    id("io.github.adokky.quick-publish")
}

version = "0.2.1"

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