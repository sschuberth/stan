import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val detektPluginVersion: String by project
val kotlinPluginVersion: String by project
val kotestVersion: String by project

plugins {
    // Apply third-party plugins.
    kotlin("jvm")

    id("com.github.ben-manes.versions")
    id("io.gitlab.arturbosch.detekt")
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
        "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:$detektPluginVersion")
    }

    detekt {
        // Only configure differences to the default.
        buildUponDefaultConfig = true
        config = files("$rootDir/.detekt.yml")

        source = files("$rootDir/buildSrc", "build.gradle.kts", "src/main/kotlin", "src/test/kotlin",
            "src/funTest/kotlin")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    sourceSets.create("funTest") {
        kotlin.sourceSets.getByName(name) {
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
        "funTestImplementation"("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    }

    configurations["funTestImplementation"].extendsFrom(configurations["testImplementation"])

    configurations.all {
        resolutionStrategy {
            // Ensure that all transitive versions of Kotlin libraries matches our version of Kotlin.
            force("org.jetbrains.kotlin:kotlin-reflect:$kotlinPluginVersion")
            force("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinPluginVersion")
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            jvmTarget = "11"
            apiVersion = "1.5"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

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
