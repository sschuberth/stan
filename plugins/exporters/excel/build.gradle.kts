plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    api(projects.lib)

    implementation(libs.kotlinReflect)
    implementation(libs.poiOoxml)

    funTestImplementation(testFixtures(projects.lib))
}
