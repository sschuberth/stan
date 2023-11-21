plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    project.subprojects.forEach {
        api(it)
    }
}
