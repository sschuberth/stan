plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    api(projects.lib)

    funTestImplementation(testFixtures(projects.lib))
}
