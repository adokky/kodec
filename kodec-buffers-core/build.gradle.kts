plugins {
    alias(libs.plugins.quick.mpp)
    alias(libs.plugins.quick.publish)
}

version = "0.6.0"

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