plugins {
    id("io.github.adokky.quick-mpp")
}

dependencies {
    commonMainApi(kotlin("test"))
    commonMainApi(project(":kodec-strings-common"))
    commonMainApi(libs.equalsTester)
    commonMainImplementation(libs.karamelUtils.core)
}