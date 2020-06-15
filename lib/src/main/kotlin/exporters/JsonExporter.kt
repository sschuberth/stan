package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.Statement

import java.io.OutputStream

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.stringify

val JSON = Json(JsonConfiguration.Stable.copy(prettyPrint = true, indent = "  "))

class JsonExporter : Exporter {
    override val extension = "json"

    @ImplicitReflectionSerializer
    override fun write(statement: Statement, output: OutputStream) =
        output.use { it.write(JSON.stringify(statement).toByteArray()) }
}
