package com.github.sschuberth.stan.exporters

import com.github.sschuberth.stan.model.Statement

import java.io.File

class JsonExporter : Exporter {
    override fun write(statement: Statement, filename: String) =
            File(filename).writeText(statement.toString())
}
