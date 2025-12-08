@file:UseSerializers(dev.schuberth.stan.utils.LocaleSerializer::class)

package dev.schuberth.stan.model

import io.ks3.java.typealiases.LocalDateAsString

import java.time.LocalDate
import java.util.Locale

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
@Suppress("LongParameterList")
data class Statement(
    val filename: String,
    val locale: Locale,
    val bankId: String,
    val accountId: String,
    val fromDate: LocalDateAsString,
    val toDate: LocalDateAsString,
    val balanceOld: Float,
    val balanceNew: Float,
    val sumIn: Float,
    val sumOut: Float,
    val bookings: List<BookingItem>
) {
    companion object {
        @JvmField
        val EMPTY = Statement(
            filename = "",
            locale = Locale.ROOT,
            bankId = "",
            accountId = "",
            fromDate = LocalDate.EPOCH,
            toDate = LocalDate.EPOCH,
            balanceOld = 0f,
            balanceNew = 0f,
            sumIn = 0f,
            sumOut = 0f,
            bookings = emptyList()
        )
    }

    init {
        require(bankId.none { it.isWhitespace() }) {
            "The bank ID must not contain any whitespace characters."
        }

        require(accountId.none { it.isWhitespace() }) {
            "The account ID must not contain any whitespace characters."
        }
    }
}
