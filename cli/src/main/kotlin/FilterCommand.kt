package dev.schuberth.stan.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum

import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.model.joinInfo

import java.time.LocalDate

class FilterCommand : CliktCommand("filter") {
    override fun help(context: Context) = "Filter booking items."

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
        help = "Keep only booking items of this type."
    ).enum<BookingType>()

    private val typeNot by option(
        "--type-not",
        help = "Remove all booking items of this type."
    ).enum<BookingType>()

    private val filterPattern by option(
        "--info-matches",
        help = "Keep only booking item whose info matches the given regular expression."
    )

    private val filterNotPattern by option(
        "--info-matches-not",
        help = "Remove all booking item whose info matches the given regular expression."
    )

    private val lessOrEqual by option(
        "--less-or-equal",
        help = "Only include bookings that are less or equal to the given amount."
    )

    private val greaterOrEqual by option(
        "--greater-or-equal",
        help = "Only include bookings that are greater or equal to the given amount."
    )

    private val statements by requireObject<Set<Statement>>()

    override fun run() {
        val fromDate = from?.let { LocalDate.parse(it) }
        val toDate = to?.let { LocalDate.parse(it) }
        val filterRegex = filterPattern?.let { Regex(it, RegexOption.IGNORE_CASE) }
        val filterNotRegex = filterNotPattern?.let { Regex(it, RegexOption.IGNORE_CASE) }

        val filteredBookings = statements
            .flatMap { it.bookings }
            .asSequence()
            .filter { fromDate?.isBefore(it.valueDate) != false || fromDate.isEqual(it.valueDate) }
            .filter { toDate?.isAfter(it.valueDate) != false }
            .filter { type == null || type == it.type }
            .filterNot { typeNot == it.type }
            .filter { filterRegex?.containsMatchIn(it.info.joinInfo()) != false }
            .filterNot { filterNotRegex?.containsMatchIn(it.info.joinInfo()) == true }
            .filter { lessOrEqual?.toFloat()?.let { threshold -> it.amount <= threshold } != false }
            .filter { greaterOrEqual?.toFloat()?.let { threshold -> it.amount >= threshold } != false }

        filteredBookings.forEach {
            println("${it.valueDate} : ${it.type} : ${"%.2f".format(it.amount)}")
            it.info.forEach { line ->
                println("    $line")
            }
            println()
        }

        val sum = filteredBookings.sumOf { it.amount.toDouble() }
        println("Sum: ${"%.2f".format(sum)}")
    }
}
