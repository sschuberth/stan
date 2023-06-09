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
    }

    private val config: Configuration by inject()

    abstract fun isApplicable(statementFile: File): Boolean

    fun parse(statementFile: File, options: Map<String, String> = emptyMap()) =
        parseInternal(statementFile, options).applyCategories(config).performSanityChecks()

    protected abstract fun parseInternal(statementFile: File, options: Map<String, String> = emptyMap()): Statement
}

private fun Statement.applyCategories(config: Configuration) =
    copy(bookings = bookings.map { it.copy(category = config.findBookingCategory(it)?.name) })

private fun Statement.performSanityChecks() =
    apply {
        var calcIn = 0.0f
        var calcOut = 0.0f
        for (item in bookings) {
            if (item.amount > 0) {
                calcIn += item.amount
            } else {
                calcOut += item.amount
            }
        }

        if (abs(calcIn - sumIn) >= 0.01) {
            throw ParseException("Sanity check on incoming booking summary failed: $calcIn != $sumIn", 0)
        }

        if (abs(calcOut - sumOut) >= 0.01) {
            throw ParseException("Sanity check on outgoing booking summary failed: $calcOut != $sumOut", 0)
        }

        val balanceCalc = balanceOld + sumIn + sumOut
        if (abs(balanceCalc - balanceNew) >= 0.01) {
            throw ParseException("Sanity check on balances failed: $balanceCalc != $balanceNew", 0)
        }
    }
