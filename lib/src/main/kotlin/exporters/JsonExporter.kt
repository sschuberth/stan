package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.Statement

import java.io.OutputStream

class JsonExporter : Exporter {
    override fun write(statement: Statement, output: OutputStream) =
            output.use { it.write(statement.toString().toByteArray()) }
}
