plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    api(libs.koinCore)
    api(projects.lib)
    api(projects.plugins.api)
}
