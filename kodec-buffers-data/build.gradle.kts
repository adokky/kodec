plugins {
    alias(libs.plugins.quick.mpp)
    alias(libs.plugins.quick.publish)
}

version = "0.10.0"

dependencies {
    commonMainApi(project(":kodec-binary-num"))
    commonMainApi(project(":kodec-strings-utf"))
    commonMainApi(project(":kodec-buffers-core"))
    commonMainImplementation(libs.karamelUtils.core)

    commonTestImplementation(project(":kodec-testing"))
}

mavenPublishing {
    pom {
        description = "`kodec-buffers-core` extensions for encoding/decoding strings and numbers"
        inceptionYear = "2025"
    }
}
