package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.utils.JSON

import java.io.OutputStream

import kotlinx.serialization.json.encodeToStream

/**
 * See https://www.json.org/.
 */
class JsonExporter : Exporter {
    override val name = "JSON"
    override val extension = "json"

    override fun write(statement: Statement, output: OutputStream, options: Map<String, String>) =
        output.use { JSON.encodeToStream(statement, it) }
}
