import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private val Project.libs: LibrariesForLibs
    get() = extensions.getByType()

val javaLanguageVersion: String by project

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.gitlab.arturbosch.detekt")
}

repositories {
    mavenCentral()
}

configurations.all {
    resolutionStrategy {
        // Ensure that all transitive versions of Kotlin libraries matches our version of Kotlin.
        force("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlinPlugin.get()}")
        force("org.jetbrains.kotlin:kotlin-script-runtime:${libs.versions.kotlinPlugin.get()}")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaLanguageVersion)
    }
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

// Note: Kotlin DSL cannot directly access configurations that are created by applying a plugin in the very same
// project, thus put configuration names in quotes to leverage lazy lookup.
dependencies {
    "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.versions.detektPlugin.get()}")
}

detekt {
    // Only configure differences to the default.
    buildUponDefaultConfig = true
    config.from(files("$rootDir/.detekt.yml"))

    source.from(fileTree(".") { include("*.gradle.kts") }, "src/funTest/kotlin")
}

tasks.withType<KotlinCompile>().configureEach {
    val hasSerialization = plugins.hasPlugin(libs.plugins.kotlinSerialization.get().pluginId)
    val maxKotlinJvmTarget = runCatching { JvmTarget.fromTarget(javaLanguageVersion) }
        .getOrDefault(enumValues<JvmTarget>().max())

    compilerOptions {
        allWarningsAsErrors = true
        if (hasSerialization) freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
        jvmTarget = maxKotlinJvmTarget
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
