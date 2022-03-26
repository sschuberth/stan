val tornadofxVersion: String by project

plugins {
    // Apply core plugins.
    application

    // Apply third-party plugins.
    id("org.openjfx.javafxplugin")
}

application {
    mainClass.set("dev.schuberth.stan.gui.MainApp")
}

javafx {
    version = 11.coerceAtLeast(JavaVersion.current().majorVersion.toInt()).toString()
    modules("javafx.controls", "javafx.graphics")
}

dependencies {
    implementation(project(":lib"))

    implementation("no.tornado:tornadofx:$tornadofxVersion")
}
