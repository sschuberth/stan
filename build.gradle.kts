import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.plugins.ide.idea.model.IdeaProject

import org.jetbrains.gradle.ext.DefaultRunConfigurationContainer
import org.jetbrains.gradle.ext.JUnit
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.RunConfiguration
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val detektPluginVersion: String by project
val kotestVersion: String by project

plugins {
    // Apply third-party plugins.
    kotlin("jvm")

    id("com.github.ben-manes.versions")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.gradle.plugin.idea-ext")
}

fun IdeaProject.settings(block: ProjectSettings.() -> Unit) =
    (this@settings as ExtensionAware).extensions.configure("settings", block)

fun ProjectSettings.runConfigurations(block: DefaultRunConfigurationContainer.() -> Unit) =
    (this@runConfigurations as ExtensionAware).extensions.configure("runConfigurations", block)

inline fun <reified T : RunConfiguration> DefaultRunConfigurationContainer.defaults(noinline block: T.() -> Unit) =
    defaults(T::class.java, block)

idea {
    project {
        settings {
            runConfigurations {
                defaults<JUnit> {
                    // Disable "condensed" multi-line diffs when running tests from the IDE to more easily accept actual
                    // results as expected results.
                    vmParameters = "-Dkotest.assertions.multi-line-diff=simple"
                }
            }
        }
    }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val nonFinalQualifiers = listOf(
        "alpha", "b", "beta", "cr", "ea", "eap", "m", "milestone", "pr", "preview", "rc"
    ).joinToString("|", "(", ")")

    val nonFinalQualifiersRegex = Regex(".*[.-]$nonFinalQualifiers[.\\d-+]*", RegexOption.IGNORE_CASE)

    gradleReleaseChannel = "current"

    rejectVersionIf {
        candidate.version.matches(nonFinalQualifiersRegex)
    }
}

allprojects {
    repositories {
        jcenter()
    }

    apply(plugin = "io.gitlab.arturbosch.detekt")

    // Note: Kotlin DSL cannot directly access configurations that are created by applying a plugin in the very same
    // project, thus put configuration names in quotes to leverage lazy lookup.
    dependencies {
        "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:$detektPluginVersion")
    }

    detekt {
        // Only configure differences to the default.
        buildUponDefaultConfig = true
        config = files("$rootDir/.detekt.yml")

        input = files("$rootDir/buildSrc", "build.gradle.kts", "src/main/kotlin", "src/test/kotlin",
            "src/funTest/kotlin")
    }

    tasks.withType<KotlinCompile>().configureEach {
        val customCompilerArgs = listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")

        kotlinOptions {
            allWarningsAsErrors = true
            jvmTarget = "1.8"
            apiVersion = "1.3"
            freeCompilerArgs = freeCompilerArgs + customCompilerArgs
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    sourceSets.create("funTest") {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDirs("src/funTest/kotlin")
            resources.srcDirs("src/funTest/resources")
        }
    }

    dependencies {
        // By default, the same version as the plugin gets resolved.
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

        // See https://github.com/gradle/gradle/blob/master/subprojects/docs/src/samples/java/withIntegrationTests/build.gradle.
        "funTestImplementation"(sourceSets["main"].output)

        "funTestImplementation"("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
        "funTestImplementation"("io.kotest:kotest-runner-console-jvm:$kotestVersion")
        "funTestImplementation"("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    }

    configurations["funTestImplementation"].extendsFrom(configurations["testImplementation"])

    val funTest by tasks.registering(Test::class) {
        description = "Runs the functional tests."
        group = "Verification"

        classpath = sourceSets["funTest"].runtimeClasspath
        testClassesDirs = sourceSets["funTest"].output.classesDirs

        testLogging {
            events = setOf(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    tasks.named("check") {
        dependsOn(funTest)
    }
}
