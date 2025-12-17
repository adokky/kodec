plugins {
    alias(libs.plugins.quick.jvm)
    alias(libs.plugins.quick.publish)
}

version = "0.2.0"

dependencies {
    api(project(":kodec-buffers-core"))
}

mavenPublishing {
    pom {
        description = "Bridge between `kodec-buffers-core` and standard Java IO abstractions"
        inceptionYear = "2025"
    }
}