plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    api(project(":lib"))

    implementation(libs.itextpdf)

    funTestImplementation(testFixtures(project(":lib")))
}
