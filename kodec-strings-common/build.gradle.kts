plugins {
    id("io.github.adokky.quick-mpp")
    id("io.github.adokky.quick-publish")
}

version = "0.1.1"

dependencies {
    commonMainImplementation(libs.karamelUtils.core)
}

mavenPublishing {
    pom {
        description = "A tiny module with common string encoding utilities"
        inceptionYear = "2025"
    }
}