plugins {
    alias(libs.plugins.quick.mpp)
    alias(libs.plugins.quick.publish)
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