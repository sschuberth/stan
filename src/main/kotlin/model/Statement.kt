package com.github.sschuberth.stan.model

import java.time.LocalDate
import java.util.Locale

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

    override fun toString(): String {
        val result = StringBuilder("[\n")

        val bookingItemIterator = bookings.iterator()
        while (bookingItemIterator.hasNext()) {
            val item = bookingItemIterator.next()
            result.append(item.toString().prependIndent("  "))
            if (bookingItemIterator.hasNext()) {
                result.append(",")
            }
            result.append("\n")
        }

        result.append("]")

        return result.toString()
    }
}
