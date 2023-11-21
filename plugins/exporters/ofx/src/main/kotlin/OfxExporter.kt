package dev.schuberth.stan.plugins.exporters.ofx

import dev.schuberth.stan.Exporter
import dev.schuberth.stan.model.BookingItem
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.model.joinInfo

import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Currency

/**
 * See https://financialdataexchange.org/FDX/About/OFX-Work-Group.aspx?a315d1c24e44=2#a315d1c24e44.
 */
class OfxExporter : Exporter {
    companion object {
        @JvmField
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")!!

        @JvmField
        val HEADER = arrayOf(
            "OFXHEADER:100",
            "DATA:OFXSGML",
            "VERSION:160",
            "SECURITY:NONE",
            "ENCODING:UTF-8",
            "CHARSET:NONE",
            "COMPRESSION:NONE",
            "OLDFILEUID:NONE",
            "NEWFILEUID:NONE"
        )

        const val INDENTATION_STRING = "    "
    }

    override val name = "OFX"
    override val extension = "ofx"

    override fun write(statement: Statement, output: OutputStream, options: Map<String, String>) {
        BufferedWriter(OutputStreamWriter(output, StandardCharsets.UTF_8)).use { writer ->
            writer.write(HEADER.joinToString("\n") + "\n\n")

            val fromDateStr = statement.fromDate.format(DateTimeFormatter.BASIC_ISO_DATE)
            val toDateStr = statement.toDate.format(DateTimeFormatter.BASIC_ISO_DATE)

            @Suppress("UsePropertyAccessSyntax")
            writer.write(
                tag(
                    "OFX",
                    tag(
                        "SIGNONMSGSRSV1",
                        tag(
                            "SONRS",
                            writeStatusAggregate(0, "INFO"),
                            data("DTSERVER", LocalDateTime.now().format(DATE_FORMATTER)),
                            data("LANGUAGE", statement.locale.getISO3Language().uppercase())
                        )
                    ),
                    tag(
                        "BANKMSGSRSV1",
                        tag(
                            "STMTTRNRS",
                            data("TRNUID", 0),
                            writeStatusAggregate(0, "INFO"),
                            tag(
                                "STMTRS",
                                data("CURDEF", Currency.getInstance(statement.locale).toString()),
                                tag(
                                    "BANKACCTFROM",
                                    data("BANKID", statement.bankId),
                                    data("ACCTID", statement.accountId),
                                    data("ACCTTYPE", "CHECKING")
                                ),
                                tag(
                                    "BANKTRANLIST",
                                    data("DTSTART", fromDateStr),
                                    data("DTEND", toDateStr),
                                    statement.bookings.joinToString("\n") { writeStatementTransaction(it) }
                                ),
                                tag(
                                    "LEDGERBAL",
                                    data("BALAMT", statement.balanceNew),
                                    data("DTASOF", toDateStr)
                                )
                            )
                        )
                    )
                )
            )

            writer.write("\n")
        }
    }
}

private fun tag(name: String, vararg contents: String) =
    "<$name>\n${contents.joinToString("\n").prependIndent(OfxExporter.INDENTATION_STRING)}\n</$name>"

private fun data(name: String, value: Any) =
    "<$name>$value"

private fun writeStatusAggregate(code: Int, severity: String) =
    tag("STATUS", "<CODE>$code", "<SEVERITY>$severity")

private fun writeStatementTransaction(item: BookingItem) =
    tag(
        "STMTTRN",
        data("DTPOSTED", item.postDate.format(DateTimeFormatter.BASIC_ISO_DATE)),
        data("MEMO", item.info.joinInfo()),
        data("TRNAMT", item.amount),
        data("TRNTYPE", item.type.name)
    )
