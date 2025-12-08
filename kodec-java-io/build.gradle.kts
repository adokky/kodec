plugins {
    alias(libs.plugins.quick.jvm)
    alias(libs.plugins.quick.publish)
}

dependencies {
    implementation(project(":kodec-buffers-core"))
}