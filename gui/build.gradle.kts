plugins {
    id("stan-kotlin-conventions")

    alias(libs.plugins.compose)
}

compose {
    desktop {
        application {
            mainClass = "dev.schuberth.stan.gui.MainKt"
        }
    }
}

dependencies {
    implementation(project(":lib"))

    implementation(compose.desktop.currentOs)
}
