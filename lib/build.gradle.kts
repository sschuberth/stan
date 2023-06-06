import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("stan-kotlin-conventions")

    `java-library`

    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(libs.bouncyCastle)
    implementation(libs.itextpdf)
    implementation(libs.koinCore)
    implementation(libs.kotlinReflect)
    implementation(libs.kotlinxSerialization)
    implementation(libs.poiOoxml)
}

tasks.withType<KotlinCompile>().configureEach {
    val customCompilerArgs = listOf(
        "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    )

    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + customCompilerArgs
    }
}
