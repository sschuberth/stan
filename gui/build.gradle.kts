plugins {
    application

    alias(libs.plugins.compose)
}

application {
    mainClass.set("dev.schuberth.stan.gui.MainKt")
}

dependencies {
    implementation(project(":lib"))

    implementation(compose.desktop.currentOs)
    implementation(libs.bundles.aurora)
}
