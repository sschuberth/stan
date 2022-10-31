package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.Statement

import java.io.OutputStream
import java.util.ServiceLoader
import java.util.SortedMap

interface Exporter {
    companion object {
        private val LOADER = ServiceLoader.load(Exporter::class.java)

        val ALL: SortedMap<String, Exporter> by lazy {
            LOADER.iterator().asSequence().associateByTo(sortedMapOf(String.CASE_INSENSITIVE_ORDER)) { it.name }
        }
    }

    val name: String
    val extension: String

    fun write(statement: Statement, output: OutputStream, options: Map<String, String> = emptyMap())
}
