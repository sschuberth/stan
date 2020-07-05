package dev.schuberth.stan.parsers

import dev.schuberth.stan.model.Configuration
import dev.schuberth.stan.model.Statement

import java.io.File

interface Parser {
    val config: Configuration

    fun parse(statementFile: File): Statement
}
