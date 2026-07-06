plugins {
    id("stan-kotlin-conventions")

    `java-library`
    `java-test-fixtures`

    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(libs.bundles.ks3)
    implementation(libs.kotlinxSerialization)
    implementation(projects.plugins.exporters.exporterApi)

    testFixturesImplementation(libs.koinCore)
    testFixturesImplementation(libs.kotestAssertionsCore)
    testFixturesImplementation(libs.kotestRunnerJunit5)
}
