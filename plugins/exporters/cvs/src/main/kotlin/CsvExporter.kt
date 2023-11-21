package dev.schuberth.stan.plugins.exporters.csv

import dev.schuberth.stan.Exporter
import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.model.joinInfo
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
            statement.bookings.forEach { booking ->
                val moneyControlType = when (booking.type) {
                    BookingType.ATM, BookingType.CHECK, BookingType.INT, BookingType.HOLD, BookingType.OTHER,
                        BookingType.POS ->
                            if (booking.amount < 0) "Ausgabe" else "Einnahme"

                    BookingType.CASH, BookingType.DEBIT, BookingType.DIRECTDEBIT, BookingType.FEE, BookingType.PAYMENT,
                        BookingType.REPEATPMT, BookingType.SRVCHG ->
                            "Ausgabe"

                    BookingType.CREDIT, BookingType.DEP, BookingType.DIRECTDEP, BookingType.DIV ->
                        "Einnahme"

                    BookingType.XFER ->
                        "Überweisung"
                }

                val values = listOf(
                    statement.accountId,
                    booking.postDate.toString(),
                    booking.valueDate.toString(),
                    booking.info.joinInfo(" / "),
                    "%.2f".format(locale, booking.amount),
                    moneyControlType
                ).map {
                    if (separator in it) "\"$it\"" else it
                }

                writer.println(values.joinToString(separator))
            }
        }
    }
}
