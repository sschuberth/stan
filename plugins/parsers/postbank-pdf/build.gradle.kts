plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    api(projects.lib)

    implementation(libs.itextpdf)

    funTestImplementation(testFixtures(projects.lib))
}
