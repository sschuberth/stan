plugins {
    application

    alias(libs.plugins.javafx)
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

    implementation(libs.tornadofx)
}
