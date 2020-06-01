package dev.schuberth.stan.parsers

import dev.schuberth.stan.model.Statement

import java.io.File

interface Parser {
    fun parse(statementFile: File): Statement
}
