package dev.schuberth.stan.exporters

import dev.schuberth.stan.UnixPrintWriter
import dev.schuberth.stan.model.Statement

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

import kotlin.math.absoluteValue

private const val TRANSACTION_REFERENCE_NUMBER_MARKER = ":20:"
private const val ACCOUNT_IDENTIFICATION_MARKER = ":25:"
private const val STATEMENT_SEQUENCE_NUMBER_MARKER = ":28C:"
private const val OPENING_FINAL_BALANCE_MARKER = ":60F:"
private const val STATEMENT_LINE_MARKER = ":61:"
private const val CLOSING_BALANCE_MARKER = ":62:"
private const val STATEMENT_LINE_NARRATIVE_MARKER = ":86:"

private const val REFERENCE_NUMBER_LENGTH = 10
private const val TRANSACTION_NUMBER_LENGTH = 16

private const val TRANSACTION_TYPE = "N"
private const val IDENTIFICATION_CODE = "TRF"

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd")!!

/**
 * See e.g. one of these
 * - https://quickstream.westpac.com.au/bankrec-docs/statements/mt940/
 * - https://sites.google.com/a/crem-solutions.de/doku/version-2012-neu/buchhaltung/03-zahlungsverkehr/05-e-banking/technische-beschreibung-der-mt940-sta-datei
 */
class Mt940Exporter : Exporter {
    override val name = "MT940"
    override val extension = "sta"

    override fun write(statement: Statement, output: OutputStream, options: Map<String, String>) {
        UnixPrintWriter(OutputStreamWriter(output, StandardCharsets.UTF_8)).use { writer ->
            val statementDate = statement.fromDate.format(DATE_FORMATTER)

            val referenceNumber = statement.hashCode().toUInt().toString().take(REFERENCE_NUMBER_LENGTH)
                .padStart(REFERENCE_NUMBER_LENGTH, '0')

            writer.println("$TRANSACTION_REFERENCE_NUMBER_MARKER$statementDate$referenceNumber")

            writer.println("$ACCOUNT_IDENTIFICATION_MARKER${statement.bankId}/${statement.accountId}")

            val statementSequenceNumber = "1/1"
            writer.println("$STATEMENT_SEQUENCE_NUMBER_MARKER$statementSequenceNumber")

            val (openingBalanceSign, openingBalance) = creditOrDebit(statement.balanceOld)
            val currency = Currency.getInstance(statement.locale)
            writer.println("$OPENING_FINAL_BALANCE_MARKER$openingBalanceSign$statementDate$currency$openingBalance")

            statement.bookings.forEach { item ->
                val valueDate = item.valueDate.format(DATE_FORMATTER)
                val (amountSign, amount) = creditOrDebit(statement.balanceOld)

                val transactionNumber = item.hashCode().toUInt().toString().take(TRANSACTION_NUMBER_LENGTH)
                    .padStart(TRANSACTION_NUMBER_LENGTH, '0')

                writer.println(
                    "$STATEMENT_LINE_MARKER$valueDate$amountSign$amount$TRANSACTION_TYPE$IDENTIFICATION_CODE" +
                            transactionNumber
                )

                val narrative = item.info.joinToString(" / ")
                writer.println("$STATEMENT_LINE_NARRATIVE_MARKER$narrative")
            }

            val (closingBalanceSign, closingBalance) = creditOrDebit(statement.balanceOld)
            writer.println("$CLOSING_BALANCE_MARKER$closingBalanceSign$statementDate$currency$closingBalance")
        }
    }
}

private fun creditOrDebit(value: Float) =
    Pair(if (value < 0) "D" else "C", String.format(Locale.GERMAN, "%.2f", value.absoluteValue))
