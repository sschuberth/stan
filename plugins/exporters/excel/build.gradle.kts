plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    api(project(":lib"))

    implementation(libs.kotlinReflect)
    implementation(libs.poiOoxml)

    funTestImplementation(testFixtures(project(":lib")))
}
