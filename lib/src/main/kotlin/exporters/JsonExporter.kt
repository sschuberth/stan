package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.Statement

import java.io.OutputStream

import kotlinx.serialization.json.Json

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
        output.use { it.write(JSON.encodeToString(Statement.serializer(), statement).toByteArray()) }
}
