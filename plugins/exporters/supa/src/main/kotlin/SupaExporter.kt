package dev.schuberth.stan.plugins.exporters.supa

import dev.schuberth.stan.Exporter
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.model.joinInfo
import dev.schuberth.stan.utils.JSON

import java.io.OutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale

import kotlin.math.absoluteValue
import kotlinx.serialization.json.encodeToStream

/**
 * See https://www.subsembly.com/download/SUPA.pdf.
 */
class SupaExporter : Exporter {
    private val currency = "EUR"

    override val name = "SUPA"
    override val extension = "json"

    override fun write(statement: Statement, output: OutputStream, options: Map<String, String>) {
        val bookings = statement.bookings.map {
            Booking(
                OwnrAcctCcy = currency,
                OwnrAcctIBAN = statement.accountId,
                OwnrAcctBIC = statement.bankId,
                BookgDt = it.postDate.format(DateTimeFormatter.ISO_DATE),
                ValDt = it.valueDate.format(DateTimeFormatter.ISO_DATE),
                Amt = "%.2f".format(Locale.ROOT, it.amount.absoluteValue),
                AmtCcy = currency,
                CdtDbtInd = CreditDebitIndicator.forAmount(it.amount),
                RmtInf = it.info.drop(1).joinInfo(),
                BookgTxt = it.info.first(),
                RmtdAcctCtry = statement.locale.country,
                RmtdAcctIBAN = statement.accountId,
                RmtdAcctBIC = statement.bankId,
                Category = it.category
            )
        }

        output.use { JSON.encodeToStream(bookings, it) }
    }
}
