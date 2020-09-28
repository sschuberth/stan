package dev.schuberth.stan.parsers

import dev.schuberth.stan.model.Configuration
import dev.schuberth.stan.model.Statement

import java.io.File

abstract class Parser(protected val config: Configuration) {
    abstract fun parse(statementFile: File, options: Map<String, String> = emptyMap()): Statement
}
