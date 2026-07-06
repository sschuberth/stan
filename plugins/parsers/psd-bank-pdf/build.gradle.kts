plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    api(projects.lib)

    implementation(libs.bouncyCastle)
    implementation(libs.itextpdf)
}
