package com.github.sschuberth.stan.exporters

import com.github.sschuberth.stan.model.Statement

import java.io.FileOutputStream
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
        override fun println() {
            write("\n")
        }
    }

    override fun write(statement: Statement, filename: String) {
        with(UnixPrintWriter(OutputStreamWriter(FileOutputStream(filename), StandardCharsets.UTF_8))) {
            val accountType = "Bank"

            println("!Account")
            println("N${statement.accountId} ${statement.bankId}")
            println("T$accountType")
            println("^")
            println("!Type:$accountType")

            statement.bookings.forEach {
                val date = it.valueDate.format(DATE_FORMATTER)
                val amount = it.amount
                val memo = it.info.joinToString()

                println("D$date")
                println("T$amount")
                println("P$memo")

                println("^")
            }

            close()
        }
    }
}
