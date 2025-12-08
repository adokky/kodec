plugins {
    kotlin("multiplatform") version libs.versions.kotlin apply false
    alias(libs.plugins.quick.mpp) apply false
    alias(libs.plugins.quick.jvm) apply false
    alias(libs.plugins.quick.publish) apply false
}

group = "io.github.adokky"

val unpublishedProjects = listOf("kodec-testing")

subprojects {
    group = rootProject.group

    if (name !in unpublishedProjects) {
        apply(plugin = "io.github.adokky.quick-publish")
    }
}