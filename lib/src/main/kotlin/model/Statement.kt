package dev.schuberth.stan.model

import java.time.LocalDate
import java.util.Locale

@Suppress("LongParameterList")
class Statement(
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

    override fun toString() =
        buildString {
            append("[\n")

            val bookingItemIterator = bookings.iterator()
            while (bookingItemIterator.hasNext()) {
                val item = bookingItemIterator.next()
                append(item.toString().prependIndent("  "))
                if (bookingItemIterator.hasNext()) {
                    append(",")
                }
                append("\n")
            }

            append("]")
        }
}
