import java.nio.file.Files

import java.nio.charset.Charset

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

val jar by tasks.getting(Jar::class)

val pathingJar by tasks.registering(Jar::class) {
    archiveClassifier = "pathing"

    manifest {
        // Work around the command line length limit on Windows when passing the classpath to Java, see
        // https://github.com/gradle/gradle/issues/1989.
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(" ") { it.name }
    }
}

distributions {
    main {
        contents {
            from(pathingJar) {
                into("lib")
            }
        }
    }
}

tasks.named<CreateStartScripts>("startScripts") {
    classpath = jar.outputs.files + pathingJar.get().outputs.files

    doLast {
        // Append the plugin directory to the Windows classpath.
        val windowsScriptText = windowsScript.readText(Charset.defaultCharset())
        windowsScript.writeText(
            windowsScriptText.replace(Regex("(set CLASSPATH=%APP_HOME%\\\\lib\\\\.*)"), "$1;%APP_HOME%\\\\plugin\\\\*")
        )

        // Append the plugin directory to the Unix classpath.
        val unixScriptText = unixScript.readText(Charset.defaultCharset())
        unixScript.writeText(
            unixScriptText.replace(Regex("(CLASSPATH=\\\$APP_HOME/lib/.*)"), "$1:\\\$APP_HOME/plugin/*")
        )
    }
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
