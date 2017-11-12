package com.github.sschuberth.stan.exporters

import com.github.sschuberth.stan.model.Statement

import java.io.PrintWriter

class JsonExporter : Exporter {
    override fun write(statement: Statement, filename: String) {
        PrintWriter(filename).use { out -> out.print(statement.toString()) }
    }
}
