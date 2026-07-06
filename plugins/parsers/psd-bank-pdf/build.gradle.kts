plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    compileOnly(projects.plugins.parsers.parserApi)

    implementation(libs.bouncyCastle)
    implementation(libs.itextpdf)
}
