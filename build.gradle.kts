plugins {
    kotlin("multiplatform") version libs.versions.kotlin apply false
    id("io.github.adokky.quick-mpp") version libs.versions.quickMpp apply false
    id("io.github.adokky.quick-publish") version libs.versions.quickMpp apply false
}

group = "io.github.adokky"

val unpublishedProjects = listOf("kodec-testing")

subprojects {
    group = rootProject.group

    if (name !in unpublishedProjects) {
        apply(plugin = "io.github.adokky.quick-publish")
    }
}