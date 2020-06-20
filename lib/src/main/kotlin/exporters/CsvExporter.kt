package dev.schuberth.stan.exporters

import dev.schuberth.stan.UnixPrintWriter
import dev.schuberth.stan.model.Statement

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

const val CSV_SEPARATOR = ','

/**
 * See https://en.wikipedia.org/wiki/Comma-separated_values.
 */
class CsvExporter : Exporter {
    override val extension = "csv"

    override fun write(statement: Statement, output: OutputStream) {
        UnixPrintWriter(OutputStreamWriter(output, StandardCharsets.UTF_8)).use { writer ->
            statement.bookings.forEach { booking ->
                val values = listOf(
                    booking.postDate.toString(),
                    booking.valueDate.toString(),
                    booking.info.joinToString(" / "),
                    booking.amount.toString(),
                    moneyControlType
                ).map {
                    if (CSV_SEPARATOR in it) "\"$it\"" else it
                }

                writer.println(values.joinToString(CSV_SEPARATOR.toString()))
            }
        }
    }
}
