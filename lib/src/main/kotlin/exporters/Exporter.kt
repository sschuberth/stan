package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.Statement

import java.io.OutputStream

interface Exporter {
    val extension: String

    fun write(statement: Statement, output: OutputStream)
}
