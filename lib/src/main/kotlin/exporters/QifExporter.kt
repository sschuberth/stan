package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.Statement

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter

class QifExporter : Exporter {
    companion object {
        @JvmField
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd''yy")!!
    }

    private class UnixPrintWriter(writer: Writer) : PrintWriter(writer) {
        override fun println() = write("\n")
    }

    override fun write(statement: Statement, output: OutputStream) {
        UnixPrintWriter(OutputStreamWriter(output, StandardCharsets.UTF_8)).use { writer ->
            val accountType = "Bank"

            writer.println("!Account")
            writer.println("N${statement.accountId} ${statement.bankId}")
            writer.println("T$accountType")
            writer.println("^")
            writer.println("!Type:$accountType")

            statement.bookings.forEach {
                val date = it.valueDate.format(DATE_FORMATTER)
                val amount = it.amount
                val memo = it.info.joinToString()

                writer.println("D$date")
                writer.println("T$amount")
                writer.println("P$memo")

                writer.println("^")
            }
        }
    }
}
