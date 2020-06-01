import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotestVersion: String by project

plugins {
    // Apply third-party plugins.
    kotlin("jvm")

    id("com.github.ben-manes.versions")
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

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            jvmTarget = "1.8"
            apiVersion = "1.3"
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

        "funTestImplementation"("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
        "funTestImplementation"("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
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
