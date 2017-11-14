package com.github.sschuberth.stan.parsers

import com.github.sschuberth.stan.model.Statement

interface Parser {
    fun parse(filename: String): Statement
}
