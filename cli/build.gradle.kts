val cliktVersion: String by project

plugins {
    // Apply core plugins.
    application
}

application {
    mainClassName = "com.github.sschuberth.stan.MainKt"
}

dependencies {
    implementation(project(":lib"))

    implementation("com.github.ajalt:clikt:$cliktVersion")
}
