plugins {
    id("stan-kotlin-conventions")

    application
}

application {
    applicationName = "stan"
    mainClass.set("dev.schuberth.stan.cli.MainKt")
}

dependencies {
    implementation(platform(project(":plugins:exporters")))

    implementation(project(":lib"))

    implementation(libs.clikt)
    implementation(libs.koinCore)
    implementation(libs.kotlinReflect)
}
