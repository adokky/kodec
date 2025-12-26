plugins {
    alias(libs.plugins.quick.mpp)
    id("org.jetbrains.kotlinx.benchmark") version "0.4.15"
    kotlin("plugin.allopen") version libs.versions.kotlin
}

dependencies {
    commonMainImplementation(project(":kodec-buffers-data"))
    commonMainImplementation(project(":kodec-strings-number"))
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.15")
}

kotlin {
//    js {
//        nodejs()
//    }
    @Suppress("OPT_IN_USAGE")
    wasmJs {
        nodejs()
    }
}

benchmark {
    targets {
        register("jvm")
        register("linuxX64")
        register("wasmJs")
//        register("js")
    }
    configurations {
        named("main") {
            advanced("jvmForks", 30)
        }
    }
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}