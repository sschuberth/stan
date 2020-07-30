val itextpdfVersion: String by project
val kotlinxSerializationVersion: String by project
val poiVersion: String by project

plugins {
    // Apply third-party plugins.
    kotlin("plugin.serialization")
}

dependencies {
    implementation("com.itextpdf:itextpdf:$itextpdfVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    // By default, the same version as the plugin gets resolved.
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")
}
