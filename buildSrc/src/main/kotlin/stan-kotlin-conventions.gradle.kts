import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private val Project.libs: LibrariesForLibs
    get() = extensions.getByType()

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.gitlab.arturbosch.detekt")
}

repositories {
    mavenCentral()
}

// Note: Kotlin DSL cannot directly access configurations that are created by applying a plugin in the very same
// project, thus put configuration names in quotes to leverage lazy lookup.
dependencies {
    "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.versions.detektPlugin.get()}")
}

detekt {
    // Only configure differences to the default.
    buildUponDefaultConfig = true
    config.from(files("$rootDir/.detekt.yml"))

    source.from(
        files(
            "$rootDir/buildSrc",
            "build.gradle.kts",
            "src/main/kotlin",
            "src/test/kotlin",
            "src/funTest/kotlin"
        )
    )
}

testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter()

            dependencies {
                implementation(libs.kotestAssertionsCore)
                implementation(libs.kotestRunnerJunit5)
            }
        }

        register<JvmTestSuite>("funTest") {
            sources {
                kotlin {
                    testType = TestSuiteType.FUNCTIONAL_TEST
                }
            }
        }
    }
}

// Associate the "funTest" compilation with the "main" compilation to be able to access "internal" objects from
// functional tests.
kotlin.target.compilations.apply {
    getByName("funTest").associateWith(getByName(KotlinCompilation.MAIN_COMPILATION_NAME))
}

configurations.all {
    resolutionStrategy {
        // Ensure that all transitive versions of Kotlin libraries matches our version of Kotlin.
        force("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlinPlugin.get()}")
        force("org.jetbrains.kotlin:kotlin-script-runtime:${libs.versions.kotlinPlugin.get()}")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    val hasSerialization = plugins.hasPlugin(libs.plugins.kotlinSerialization.get().pluginId)

    kotlinOptions {
        allWarningsAsErrors = true

        if (hasSerialization) {
            freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        }

        jvmTarget = JavaVersion.current().majorVersion.toInt().coerceAtMost(19).toString()
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events = setOf(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = false
    }
}

tasks.named("check") {
    dependsOn(tasks["funTest"])
}
