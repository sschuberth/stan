package dev.schuberth.stan

import dev.schuberth.stan.model.Configuration
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.utils.NamedPlugin

import java.io.File
import java.text.ParseException

import kotlin.math.abs

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class Parser : KoinComponent, NamedPlugin {
    companion object {
        @JvmField
        val ALL = NamedPlugin.getAll<Parser>()

        const val IBAN_LENGTH = 22
    }

    private val config: Configuration by inject()

    abstract fun isApplicable(statementFile: File): Boolean

    fun parse(statementFile: File, options: Map<String, String> = emptyMap()) =
        parseInternal(statementFile, options).applyCategories(config).performSanityChecks()

    protected abstract fun parseInternal(statementFile: File, options: Map<String, String> = emptyMap()): Statement
}

private fun Statement.applyCategories(config: Configuration) = copy(
    bookings = bookings.map { item ->
        val configuredCategory = config.findBookingCategory(item)?.name
        item.takeUnless { configuredCategory != null } ?: item.copy(category = configuredCategory)
    }
)

private fun Statement.performSanityChecks(tolerance: Float = 0.01f) = apply {
    var calcIn = 0.0f
    var calcOut = 0.0f
    for (item in bookings) {
        if (item.amount > 0) {
            calcIn += item.amount
        } else {
            calcOut += item.amount
        }
    }

    if (abs(calcIn - sumIn) > tolerance) {
        throw ParseException("Sanity check on incoming booking summary failed: $calcIn != $sumIn", 0)
    }

    if (abs(calcOut - sumOut) > tolerance) {
        throw ParseException("Sanity check on outgoing booking summary failed: $calcOut != $sumOut", 0)
    }

    val balanceCalc = balanceOld + sumIn + sumOut
    if (abs(balanceCalc - balanceNew) > tolerance) {
        throw ParseException("Sanity check on balances failed: $balanceCalc != $balanceNew", 0)
    }
}
