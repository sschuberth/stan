import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("stan-kotlin-conventions")

    `java-library`
    `java-test-fixtures`

    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    api(libs.koinCore)

    implementation(libs.bundles.ks3)
    implementation(libs.kotlinxSerialization)

    testFixturesImplementation(libs.koinCore)
    testFixturesImplementation(libs.kotestAssertionsCore)
    testFixturesImplementation(libs.kotestRunnerJunit5)
}

// Must not opt-in for "compileTestFixturesKotlin" as it does not have kotlinx-serialization in the classpath.
listOf("compileKotlin", "compileTestKotlin").forEach {
    tasks.named<KotlinCompile>(it) {
        val customCompilerArgs = listOf(
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )

        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + customCompilerArgs
        }
    }
}
