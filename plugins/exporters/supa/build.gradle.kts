plugins {
    id("stan-kotlin-conventions")

    `java-library`

    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    compileOnly(projects.plugins.exporters.exporterApi)

    implementation(libs.kotlinxSerialization)

    funTestImplementation(testFixtures(projects.lib))
}
