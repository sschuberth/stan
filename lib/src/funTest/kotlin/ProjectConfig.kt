package dev.schuberth.stan

import dev.schuberth.stan.model.Configuration

import io.kotest.core.config.AbstractProjectConfig

import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class ProjectConfig : AbstractProjectConfig() {
    init {
        val configModule = module {
            single { Configuration.loadDefault() }
        }

        startKoin {
            modules(configModule)
        }

        val isRunningFromIdea = "idea_rt.jar" in System.getProperty("java.class.path")
        if (isRunningFromIdea) System.setProperty("kotest.assertions.multi-line-diff", "simple")
    }
}
