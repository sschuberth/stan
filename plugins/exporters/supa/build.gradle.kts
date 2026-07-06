plugins {
    id("stan-kotlin-conventions")

    `java-library`

    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    api(projects.lib)

    implementation(libs.kotlinxSerialization)

    funTestImplementation(testFixtures(projects.lib))
}
