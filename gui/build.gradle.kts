val kotlinPluginVersion: String by project
val tornadofxVersion: String by project

plugins {
    // Apply core plugins.
    application

    // Apply third-party plugins.
    id("org.openjfx.javafxplugin")
}

application {
    mainClassName = "dev.schuberth.stan.gui.MainApp"
}

javafx {
    version = JavaVersion.current().majorVersion
    modules("javafx.controls", "javafx.graphics")
}

configurations.all {
    resolutionStrategy {
        // Ensure that tornadofx' version of "kotlin-reflect" matches our version of Kotlin.
        force("org.jetbrains.kotlin:kotlin-reflect:$kotlinPluginVersion")
    }
}

dependencies {
    implementation(project(":lib"))

    implementation("no.tornado:tornadofx:$tornadofxVersion")
}
