plugins {
    id("stan-kotlin-conventions")

    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

compose {
    desktop {
        application {
            mainClass = "dev.schuberth.stan.gui.MainKt"
        }
    }
}

repositories {
    google()
}

dependencies {
    implementation(project(":lib"))

    implementation(compose.desktop.currentOs)
}
