plugins {
    id("stan-kotlin-conventions")

    `java-library`
}

dependencies {
    api(project(":lib"))

    implementation(libs.bouncyCastle)
    implementation(libs.itextpdf)
}
