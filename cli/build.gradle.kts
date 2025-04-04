import java.nio.file.Files

import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask

plugins {
    id("stan-kotlin-conventions")

    application

    alias(libs.plugins.graalvm)
}

application {
    applicationName = "stan"
    mainClass = "dev.schuberth.stan.cli.MainKt"
}

dependencies {
    implementation(platform(project(":plugins:exporters")))
    implementation(platform(project(":plugins:parsers")))

    implementation(project(":lib"))

    implementation(libs.clikt)
    implementation(libs.koinCore)
    implementation(libs.kotlinReflect)
}

graalvmNative {
    toolchainDetection = System.getenv("GRAALVM_HOME") == null

    metadataRepository {
        enabled = true
    }

    // For options see https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html.
    binaries {
        named("main") {
            imageName = provider { application.applicationName }

            buildArgs.addAll(
                "--parallelism=8",
                "-J-Xmx16g",
                "-Os"
            )
        }
    }
}

tasks.named<BuildNativeImageTask>("nativeCompile") {
    // Gradle's "Copy" task cannot handle symbolic links, see https://github.com/gradle/gradle/issues/3982. That is why
    // links contained in the GraalVM distribution archive get broken during provisioning and are replaced by empty
    // files. Address this by recreating the links in the toolchain directory.
    val toolchainDir = System.getenv("GRAALVM_HOME")?.let { File(it) }
        ?: options.get().javaLauncher.get().executablePath.asFile.parentFile.run {
            if (name == "bin") parentFile else this
        }

    val toolchainFiles = toolchainDir.walkTopDown().filter { it.isFile }
    val emptyFiles = toolchainFiles.filter { it.length() == 0L }

    // Find empty toolchain files that are named like other toolchain files and assume these should have been links.
    val links = toolchainFiles.mapNotNull { file ->
        emptyFiles.singleOrNull { it != file && it.name == file.name }?.let {
            file to it
        }
    }

    // Fix up symbolic links.
    links.forEach { (target, link) ->
        logger.quiet("Fixing up '$link' to link to '$target'.")

        if (link.delete()) {
            Files.createSymbolicLink(link.toPath(), target.toPath())
        } else {
            logger.warn("Unable to delete '$link'.")
        }
    }
}
