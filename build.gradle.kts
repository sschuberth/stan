import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.versionCatalogUpdate)
    alias(libs.plugins.versions)
}

versionCatalogUpdate {
    // Keep the custom sorting / grouping.
    sortByKey.set(false)
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val nonFinalQualifiers = listOf(
        "alpha", "b", "beta", "cr", "dev", "ea", "eap", "m", "milestone", "pr", "preview", "rc"
    ).joinToString("|", "(", ")")

    val nonFinalQualifiersRegex = Regex(".*[.-]$nonFinalQualifiers[.\\w-+]*", RegexOption.IGNORE_CASE)

    gradleReleaseChannel = "current"

    rejectVersionIf {
        candidate.version.matches(nonFinalQualifiersRegex)
    }
}

allprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "io.gitlab.arturbosch.detekt")

    // Note: Kotlin DSL cannot directly access configurations that are created by applying a plugin in the very same
    // project, thus put configuration names in quotes to leverage lazy lookup.
    dependencies {
        "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:${rootProject.libs.versions.detektPlugin.get()}")
    }

    detekt {
        // Only configure differences to the default.
        buildUponDefaultConfig = true
        config = files("$rootDir/.detekt.yml")

        source = files(
            "$rootDir/buildSrc",
            "build.gradle.kts",
            "src/main/kotlin",
            "src/test/kotlin",
            "src/funTest/kotlin"
        )
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    testing {
        suites {
            register<JvmTestSuite>("funTest") {
                sources {
                    kotlin {
                        testType.set(TestSuiteType.FUNCTIONAL_TEST)
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

    configurations["funTestImplementation"].extendsFrom(configurations["testImplementation"])

    configurations.all {
        resolutionStrategy {
            // Ensure that all transitive versions of Kotlin libraries matches our version of Kotlin.
            force("org.jetbrains.kotlin:kotlin-reflect:${rootProject.libs.versions.kotlinPlugin.get()}")
            force("org.jetbrains.kotlin:kotlin-script-runtime:${rootProject.libs.versions.kotlinPlugin.get()}")
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            jvmTarget = "11"
            apiVersion = "1.7"
        }
    }

    tasks.withType<Test>().configureEach {
        testLogging {
            events = setOf(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
        }

        useJUnitPlatform()
    }

    tasks.named("check") {
        dependsOn(tasks["funTest"])
    }
}
