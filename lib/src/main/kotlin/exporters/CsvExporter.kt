package dev.schuberth.stan.exporters

import dev.schuberth.stan.UnixPrintWriter
import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.model.Statement

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale

const val CVS_DEFAULT_SEPARATOR = ","

/**
 * See https://en.wikipedia.org/wiki/Comma-separated_values.
 */
class CsvExporter : Exporter {
    override val extension = "csv"

    override fun write(statement: Statement, output: OutputStream, options: Map<String, String>) {
        val locale = options["locale"]?.let { Locale(it) } ?: Locale.getDefault()
        val separator = options["separator"] ?: CVS_DEFAULT_SEPARATOR

        UnixPrintWriter(OutputStreamWriter(output, StandardCharsets.UTF_8)).use { writer ->
            statement.bookings.forEach { booking ->
                val moneyControlType = when (booking.type) {
                    BookingType.ATM, BookingType.CHECK, BookingType.INT ->
                        if (booking.amount < 0) "Ausgabe" else "Einnahme"

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
                    booking.postDate.toString(),
                    booking.valueDate.toString(),
                    booking.info.joinToString(" / "),
                    String.format(locale, "%.02f", booking.amount),
                    moneyControlType
                ).map {
                    if (separator in it) "\"$it\"" else it
                }

                writer.println(values.joinToString(separator))
            }
        }
    }
}
