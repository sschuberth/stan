package dev.schuberth.stan.parsers

import dev.schuberth.stan.model.Configuration
import dev.schuberth.stan.model.Statement

import java.io.File
import java.util.ServiceLoader
import java.util.SortedMap

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class Parser : KoinComponent {
    companion object {
        private val LOADER = ServiceLoader.load(Parser::class.java)

        val ALL: SortedMap<String, Parser> by lazy {
            LOADER.iterator().asSequence().associateByTo(sortedMapOf(String.CASE_INSENSITIVE_ORDER)) { it.name }
        }
    }

    private val config: Configuration by inject()

    abstract val name: String

    fun parse(statementFile: File, options: Map<String, String> = emptyMap()) =
        parseInternal(statementFile, options).applyCategories(config)

    protected abstract fun parseInternal(statementFile: File, options: Map<String, String> = emptyMap()): Statement
}

private fun Statement.applyCategories(config: Configuration) =
    copy(bookings = bookings.map { it.copy(category = config.findBookingCategory(it)?.name) })
