plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    compileOnly(projects.plugins.exporters.exporterApi)

    funTestImplementation(testFixtures(projects.lib))
}
