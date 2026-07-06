plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    compileOnly(projects.plugins.exporters.exporterApi)

    implementation(libs.kotlinReflect)
    implementation(libs.poiOoxml)

    funTestImplementation(testFixtures(projects.lib))
}
