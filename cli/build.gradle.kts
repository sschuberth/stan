plugins {
    application
}

application {
    applicationName = "stan"
    mainClassName = "dev.schuberth.stan.cli.MainKt"
}

dependencies {
    implementation(project(":lib"))

    implementation(libs.clikt)
    implementation(libs.kotlinReflect)
}
