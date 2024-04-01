include(":cli")
include(":gui")
include(":lib")

file("plugins").walk().maxDepth(3).filter {
    it.isFile && it.name == "build.gradle.kts"
}.mapTo(mutableListOf()) {
    it.parentFile.toRelativeString(rootDir).replace(File.separatorChar, ':')
}.forEach {
    include(":$it")

    val parts = it.split(':')
    if (parts.size == 3) project(":$it").name = "${parts[2]}-${parts[1].removeSuffix("s")}"
}

plugins {
    // Gradle cannot access the version catalog from here, so hard-code the dependency.
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.8.0")
}
