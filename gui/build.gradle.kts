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

dependencies {
    implementation(project(":lib"))

    implementation("no.tornado:tornadofx:$tornadofxVersion")
}
