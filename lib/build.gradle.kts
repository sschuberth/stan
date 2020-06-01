val itextpdfVersion: String by project
val kotlinxSerializationVersion: String by project

plugins {
    // Apply third-party plugins.
    kotlin("plugin.serialization")
}

dependencies {
    implementation("com.itextpdf:itextpdf:$itextpdfVersion")

    funTestImplementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")
}
