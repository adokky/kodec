plugins {
    alias(libs.plugins.quick.mpp)
}

dependencies {
    commonMainApi(kotlin("test"))
    commonMainApi(project(":kodec-strings-common"))
    commonMainApi(libs.equalsTester)
    commonMainImplementation(libs.karamelUtils.core)
}