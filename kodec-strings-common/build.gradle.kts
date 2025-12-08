plugins {
    alias(libs.plugins.quick.mpp)
    alias(libs.plugins.quick.publish)
}

version = "0.1.2"

dependencies {
    commonMainImplementation(libs.karamelUtils.core)
}

mavenPublishing {
    pom {
        description = "A tiny module with common string encoding utilities"
        inceptionYear = "2025"
    }
}