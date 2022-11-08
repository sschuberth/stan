package dev.schuberth.stan.parsers

import dev.schuberth.stan.model.Configuration
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.utils.NamedPlugin

import java.io.File

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class Parser : KoinComponent, NamedPlugin {
    companion object {
        val ALL = NamedPlugin.getAll<Parser>()
    }

    private val config: Configuration by inject()

    abstract fun isApplicable(statementFile: File): Boolean

    fun parse(statementFile: File, options: Map<String, String> = emptyMap()) =
        parseInternal(statementFile, options).applyCategories(config)

    protected abstract fun parseInternal(statementFile: File, options: Map<String, String> = emptyMap()): Statement
}

private fun Statement.applyCategories(config: Configuration) =
    copy(bookings = bookings.map { it.copy(category = config.findBookingCategory(it)?.name) })
