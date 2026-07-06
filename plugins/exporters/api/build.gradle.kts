plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    api(projects.lib)
    api(projects.plugins.api)
}
