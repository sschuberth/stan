package dev.schuberth.stan

import dev.schuberth.stan.model.ConfigurationFile

import io.kotest.core.config.AbstractProjectConfig

import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class ProjectConfig : AbstractProjectConfig() {
    init {
        val configModule = module {
            single { ConfigurationFile.loadDefault() }
        }

        startKoin {
            modules(configModule)
        }
    }
}
