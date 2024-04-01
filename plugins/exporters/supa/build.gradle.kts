plugins {
    id("stan-kotlin-conventions")

    `java-library`

    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    api(project(":lib"))

    implementation(libs.kotlinxSerialization)

    funTestImplementation(testFixtures(project(":lib")))
}
