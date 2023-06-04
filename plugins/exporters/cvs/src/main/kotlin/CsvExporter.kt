package dev.schuberth.stan.plugins.exporters.csv

import dev.schuberth.stan.Exporter
import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.utils.UnixPrintWriter

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale

const val CVS_DEFAULT_SEPARATOR = ","

/**
 * See https://en.wikipedia.org/wiki/Comma-separated_values.
 */
class CsvExporter : Exporter {
    override val name = "CSV"
    override val extension = "csv"

    override fun write(statement: Statement, output: OutputStream, options: Map<String, String>) {
        val locale = options["locale"]?.let { Locale(it) } ?: Locale.getDefault()
        val separator = options["separator"] ?: CVS_DEFAULT_SEPARATOR

        UnixPrintWriter(OutputStreamWriter(output, StandardCharsets.UTF_8)).use { writer ->
            statement.bookings.forEach { (postDate, valueDate, info, amount, type) ->
                val moneyControlType = when (type) {
                    BookingType.ATM, BookingType.CHECK, BookingType.INT ->
                        if (amount < 0) "Ausgabe" else "Einnahme"

                    BookingType.CASH, BookingType.DEBIT, BookingType.PAYMENT, BookingType.REPEATPMT ->
                        "Ausgabe"

                    BookingType.CREDIT, BookingType.SALARY ->
                        "Einnahme"

                    BookingType.TRANSFER ->
                        "Überweisung"

                    BookingType.UNKNOWN ->
                        "Ausgabe"
                }

                val values = listOf(
                    statement.accountId,
                    postDate.toString(),
                    valueDate.toString(),
                    info.joinToString(" / "),
                    "%.2f".format(locale, amount),
                    moneyControlType
                ).map {
                    if (separator in it) "\"$it\"" else it
                }

                writer.println(values.joinToString(separator))
            }
        }
    }
}
