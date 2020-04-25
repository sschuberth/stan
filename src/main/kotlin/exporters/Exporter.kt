package com.github.sschuberth.stan.exporters

import com.github.sschuberth.stan.model.Statement

import java.io.OutputStream

interface Exporter {
    fun write(statement: Statement, output: OutputStream)
}
