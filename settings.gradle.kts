enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "stan"

include(":cli")
include(":gui")
include(":lib")

file("plugins").walk().maxDepth(3).filter {
    it.isFile && it.name == "build.gradle.kts"
}.mapTo(mutableListOf()) {
    it.parentFile.toRelativeString(rootDir).replace(File.separatorChar, ':')
}.forEach { projectPath ->
    include(":$projectPath")

    // Give API and subprojects of a type a dedicated name.
    val parts = projectPath.split(':', limit = 3)
    val projectName = parts.last()
    if (parts.size == 3) {
        // Convert the plural name for the type of plugin to singular.
        val singularTypeName = parts[1].removeSuffix("s")

        project(":$projectPath").name = when(projectName) {
            "api" -> "$singularTypeName-api"
            else -> "$projectName-$singularTypeName"
        }
    }
}

plugins {
    // Gradle cannot access the version catalog from here, so hard-code the dependency.
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}
