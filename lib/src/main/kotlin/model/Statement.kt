@file:UseSerializers(LocaleSerializer::class, LocalDateSerializer::class)

package dev.schuberth.stan.model

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
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val balanceOld: Float,
    val balanceNew: Float,
    val sumIn: Float,
    val sumOut: Float,
    val bookings: MutableList<BookingItem>
) : Comparable<Statement> {
    override fun compareTo(other: Statement) = fromDate.compareTo(other.fromDate)
}
