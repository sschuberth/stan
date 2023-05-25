plugins {
    alias(libs.plugins.compose)
}

repositories {
    exclusiveContent {
        forRepository {
            maven("https://androidx.dev/storage/compose-compiler/repository")
        }

        filter {
            includeGroup("androidx.compose.compiler")
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "dev.schuberth.stan.gui.MainKt"
        }
    }

    // See https://androidx.dev/storage/compose-compiler/repository
    // and https://github.com/JetBrains/compose-jb/blob/master/VERSIONING.md#using-jetpack-compose-compiler.
    kotlinCompilerPlugin.set("androidx.compose.compiler:compiler:1.4.1-dev-k1.8.10-c312d77f4cb")
}

dependencies {
    implementation(project(":lib"))

    implementation(compose.desktop.currentOs)
}
