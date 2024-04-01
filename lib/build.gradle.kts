plugins {
    id("stan-kotlin-conventions")

    `java-library`
    `java-test-fixtures`

    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    api(libs.koinCore)

    implementation(libs.bundles.ks3)
    implementation(libs.kotlinxSerialization)

    testFixturesImplementation(libs.koinCore)
    testFixturesImplementation(libs.kotestAssertionsCore)
    testFixturesImplementation(libs.kotestRunnerJunit5)
}
