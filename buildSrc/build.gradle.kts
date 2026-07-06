import org.gradle.accessors.dm.LibrariesForLibs

private val Project.libs: LibrariesForLibs
    get() = extensions.getByType()

plugins {
    // Use Kotlin DSL to write precompiled script plugins.
    `kotlin-dsl`
}

repositories {
    // Allow to resolve external plugins from precompiled script plugins.
    gradlePluginPortal()
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.plugin.detekt)
    implementation(libs.plugin.kotlin)
}
