plugins {
    id("io.github.adokky.quick-mpp")
    id("io.github.adokky.quick-publish")
}

version = "0.5.1"

dependencies {
    commonMainImplementation(project(":kodec-strings-common"))
    commonMainImplementation(libs.karamelUtils.core)

    commonTestImplementation(project(":kodec-testing"))
}

mavenPublishing {
    pom {
        description = "Simple array-like structure interface"
        inceptionYear = "2025"
    }
}