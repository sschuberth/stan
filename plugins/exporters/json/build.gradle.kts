import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    api(project(":lib"))

    implementation(libs.kotlinxSerialization)

    funTestImplementation(testFixtures(project(":lib")))
}

tasks.withType<KotlinCompile>().configureEach {
    val customCompilerArgs = listOf(
        "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    )

    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + customCompilerArgs
    }
}
