package com.github.sschuberth.stan.exporters

import com.github.sschuberth.stan.model.Statement

import java.io.OutputStream

class JsonExporter : Exporter {
    override fun write(statement: Statement, output: OutputStream) =
            output.use { it.write(statement.toString().toByteArray()) }
}
