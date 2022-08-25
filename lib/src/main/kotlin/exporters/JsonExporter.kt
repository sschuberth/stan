package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.Statement

import java.io.OutputStream

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream

val JSON = Json {
    encodeDefaults = false
    prettyPrint = true
    prettyPrintIndent = "  "
}

/**
 * See https://www.json.org/.
 */
class JsonExporter : Exporter {
    override val extension = "json"

    override fun write(statement: Statement, output: OutputStream, options: Map<String, String>) =
        output.use { JSON.encodeToStream(statement, it) }
}
