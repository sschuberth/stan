plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    projectDir.walk().maxDepth(2).filter {
        it.parentFile != projectDir && it.isFile && it.name == "build.gradle.kts"
    }.mapTo(mutableListOf()) {
        it.parentFile.toRelativeString(rootDir).replace(File.separatorChar, ':')
    }.forEach {
        api(project(":$it-${projectDir.name.removeSuffix("s")}"))
    }
}
