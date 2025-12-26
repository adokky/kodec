plugins {
    alias(libs.plugins.quick.mpp)
    alias(libs.plugins.quick.publish)
}

version = "0.10.0"

dependencies {
    commonMainApi(project(":kodec-strings-common"))
    commonMainApi(project(":kodec-buffers-core"))
    commonTestImplementation(project(":kodec-testing"))
    commonMainImplementation(libs.karamelUtils.core)
}

mavenPublishing {
    pom {
        description = "Convert numbers to string and back (for both stream and random access structures)"
        inceptionYear = "2025"
    }
}

val jvm21Main by sourceSets.creating {
    java.setSrcDirs(listOf("jvm21Main"))
    resources.setSrcDirs(listOf("jvm21MainRes"))
}

val compileJvm21 by tasks.registering(JavaCompile::class) {
    source = jvm21Main.java
    classpath = files()
    destinationDirectory = layout.buildDirectory.dir("classes/java/jvm21")
    options.compilerArgs.addAll(listOf(
        "--enable-preview",
        "-Xlint:preview"
    ))
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
    options.release = 21
}

// test Multi-Release JAR
tasks.register<Test>("jvm21Test") {
    dependsOn(tasks.jvmJar)

    classpath = files(
        tasks.jvmJar.get().archiveFile,
        kotlin.jvm().compilations["test"].output.allOutputs,
        configurations.jvmTestRuntimeClasspath.get()
            .filter { !(it.isDirectory && it.name.contains("main", ignoreCase = true)) }
    )

    testClassesDirs = kotlin.jvm().compilations["test"].output.classesDirs

    useJUnit()

    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jvmJar {
    dependsOn(compileJvm21)
    from(compileJvm21.get().destinationDirectory) {
        into("META-INF/versions/21")
        include("**/*.class")
        exclude("META-INF")
    }
    manifest {
        attributes("Multi-Release" to "true")
    }
}