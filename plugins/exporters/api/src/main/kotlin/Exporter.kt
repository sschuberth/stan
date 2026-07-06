package dev.schuberth.stan.plugins.exporters.api

import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.plugins.api.NamedPlugin

import java.io.OutputStream

interface Exporter : NamedPlugin {
    companion object {
        @JvmField
        val ALL = NamedPlugin.getAll<Exporter>()
    }

    val extension: String

    fun write(statement: Statement, output: OutputStream, options: Map<String, String> = emptyMap())
}
