val cliktVersion: String by project

plugins {
    // Apply core plugins.
    application
}

application {
    mainClassName = "dev.schuberth.stan.MainKt"
}

dependencies {
    implementation(project(":lib"))

    implementation("com.github.ajalt:clikt:$cliktVersion")
}
