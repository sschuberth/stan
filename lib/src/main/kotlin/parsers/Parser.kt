package dev.schuberth.stan.parsers

import dev.schuberth.stan.model.Configuration
import dev.schuberth.stan.model.Statement

import java.io.File

abstract class Parser(private val config: Configuration) {
    fun parse(statementFile: File, options: Map<String, String> = emptyMap()) =
        parseInternal(statementFile, options).applyCategories(config)

    protected abstract fun parseInternal(statementFile: File, options: Map<String, String> = emptyMap()): Statement
}

private fun Statement.applyCategories(config: Configuration) =
    copy(bookings = bookings.map { it.copy(category = config.findBookingCategory(it)?.name) })
