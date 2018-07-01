package com.github.sschuberth.stan.parsers

import com.github.sschuberth.stan.model.Statement

import java.io.File

interface Parser {
    fun parse(statementFile: File): Statement
}
