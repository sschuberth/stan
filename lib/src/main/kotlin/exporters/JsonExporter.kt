package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.Statement

import java.io.OutputStream

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

val JSON = Json(JsonConfiguration.Stable.copy(prettyPrint = true, indent = "  "))

/**
 * See https://www.json.org/.
 */
class JsonExporter : Exporter {
    override val extension = "json"

    override fun write(statement: Statement, output: OutputStream, options: Map<String, String>) =
        output.use { it.write(JSON.stringify(Statement.serializer(), statement).toByteArray()) }
}
