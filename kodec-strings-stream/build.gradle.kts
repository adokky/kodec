plugins {
    id("io.github.adokky.quick-mpp")
    id("io.github.adokky.quick-publish")
}

version = "0.8"

dependencies {
    commonMainApi(project(":kodec-buffers-core"))

    commonMainImplementation(project(":kodec-strings-number"))
    commonMainImplementation(project(":kodec-strings-utf"))
    commonMainImplementation(project(":kodec-struct"))
    
    commonMainImplementation(libs.objectPool)
    commonMainImplementation(libs.karamelUtils.core)
    commonMainImplementation(libs.karamelUtils.tsbits)

    commonTestImplementation(project(":kodec-testing"))
}

mavenPublishing {
    pom {
        description = "Streaming encoder/decoder for strings, numbers, characters"
        inceptionYear = "2025"
    }
}