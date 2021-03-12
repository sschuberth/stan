package dev.schuberth.stan

import io.kotest.core.config.AbstractProjectConfig

class ProjectConfig : AbstractProjectConfig() {
    init {
        val isRunningFromIdea = "idea_rt.jar" in System.getProperty("java.class.path")
        if (isRunningFromIdea) System.setProperty("kotest.assertions.multi-line-diff", "simple")
    }
}
