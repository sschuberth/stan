plugins {
    application

    alias(libs.plugins.javafx)
}

application {
    mainClass.set("dev.schuberth.stan.gui.MainApp")
}

javafx {
    modules("javafx.controls", "javafx.graphics")
}

dependencies {
    implementation(project(":lib"))

    implementation(libs.tornadofx)
}
