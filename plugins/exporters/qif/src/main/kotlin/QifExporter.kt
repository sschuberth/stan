package dev.schuberth.stan.plugins.exporters.qif

import dev.schuberth.stan.Exporter
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.model.joinInfo
import dev.schuberth.stan.utils.UnixPrintWriter

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter

/**
 * See https://en.wikipedia.org/wiki/Quicken_Interchange_Format.
 */
class QifExporter : Exporter {
    companion object {
        @JvmField
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd''yy")!!
    }

    override val name = "QIF"
    override val extension = "qif"

    override fun write(statement: Statement, output: OutputStream, options: Map<String, String>) {
        UnixPrintWriter(OutputStreamWriter(output, StandardCharsets.UTF_8)).use { writer ->
            val accountType = "Bank"

            writer.println("!Account")
            writer.println("N${statement.accountId} ${statement.bankId}")
            writer.println("T$accountType")
            writer.println("^")
            writer.println("!Type:$accountType")

            statement.bookings.forEach {
                val date = it.valueDate.format(DATE_FORMATTER)
                val amount = "%.2f".format(it.amount)
                val memo = it.info.joinInfo()

                writer.println("D$date")
                writer.println("T$amount")
                writer.println("M$memo")

                it.category?.also { category ->
                    writer.println("L$category")
                }

                writer.println("^")
            }
        }
    }
}
