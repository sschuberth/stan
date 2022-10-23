import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(libs.bouncyCastle)
    implementation(libs.itextpdf)
    implementation(libs.koinCore)
    implementation(libs.kotlinReflect)
    implementation(libs.kotlinxSerialization)
    implementation(libs.poiOoxml)

    funTestImplementation(libs.bundles.kotest)
}

tasks.withType<KotlinCompile>().configureEach {
    val customCompilerArgs = listOf(
        "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
    )

    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + customCompilerArgs
    }
}
