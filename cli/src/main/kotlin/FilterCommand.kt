package dev.schuberth.stan.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum

import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.model.Statement

import java.time.LocalDate
import java.util.Locale

class FilterCommand : CliktCommand(name = "filter", help = "Filter booking items.") {
    private val from by option(
        "--from",
        help = "Start date (inclusive), e.g. '2022-09-01'."
    )

    private val to by option(
        "--to",
        help = "End date (exclusive), e.g. '2023-01-01'."
    )

    private val type by option(
        "--type",
        help = "Filter for booking items of this type."
    ).enum<BookingType>()

    private val filterPattern by option(
        "--info-matches",
        help = "Keep only booking item whose info matches the given regular expression."
    )

    private val filterNotPattern by option(
        "--info-matches-not",
        help = "Remove all booking item whose info matches the given regular expression."
    )

    private val statements by requireObject<List<Statement>>()

    override fun run() {
        val fromDate = from?.let { LocalDate.parse(it) }
        val toDate = to?.let { LocalDate.parse(it) }
        val filterRegex = filterPattern?.let { Regex(it) }
        val filterNotRegex = filterNotPattern?.let { Regex(it) }

        val filteredBookings = statements
            .flatMap { it.bookings }
            .asSequence()
            .filter { fromDate?.isBefore(it.valueDate) != false || fromDate.isEqual(it.valueDate) }
            .filter { toDate?.isAfter(it.valueDate) != false }
            .filter { type == null || it.type == type }
            .filter { filterRegex?.containsMatchIn(it.joinedInfo) != false }
            .filterNot { filterNotRegex?.containsMatchIn(it.joinedInfo) == true }

        filteredBookings.forEach {
            println("${it.valueDate} : ${it.type} : ${String.format(Locale.ROOT, "%.2f", it.amount)}")
            it.info.forEach { line ->
                println("    $line")
            }
            println()
        }

        val sum = filteredBookings.sumOf { it.amount.toDouble() }
        println("Sum: ${String.format(Locale.ROOT, "%.2f", sum)}")
    }
}
