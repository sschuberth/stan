package com.github.sschuberth.stan.exporters

import com.github.sschuberth.stan.model.Statement

interface Exporter {
    fun write(statement: Statement, filename: String)
}
