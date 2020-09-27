val cliktVersion: String by project

plugins {
    // Apply core plugins.
    application
}

application {
    applicationName = "stan"
    mainClassName = "dev.schuberth.stan.cli.MainKt"
}

dependencies {
    implementation(project(":lib"))

    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")

    // By default, the same version as the plugin gets resolved.
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}
