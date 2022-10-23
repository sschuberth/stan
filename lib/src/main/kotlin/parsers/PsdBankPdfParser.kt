package dev.schuberth.stan.parsers

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor

import dev.schuberth.stan.model.BookingItem
import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.utils.Logger
import dev.schuberth.stan.utils.whenMatch

import java.io.File
import java.text.NumberFormat
import java.text.ParseException
import java.time.LocalDate
import java.util.Locale

import kotlin.IllegalArgumentException

class PsdBankPdfParser : Logger, Parser() {
    private data class ParsingState(
        var accountId: String? = null,
        var bankId: String? = null,

        var from: Balancing? = null,
        var to: Balancing? = null,

        var postDay: Int? = null,
        var postMonth: Int? = null,
        var valueDay: Int? = null,
        var valueMonth: Int? = null,
        var amount: Float? = null,
        var type: BookingType? = null
    )

    private data class Balancing(
        var date: LocalDate? = null,
        var balance: Float? = null
    )

    override val name = "PSDBankPDF"

    private val applicableProducers = setOf(
        "Compart MFFPDF I/O Filter",
        "iText 2.1.5 (by lowagie.com)",
        "iText 2.1.7 by 1T3XT"
    )

    private val locale = Locale.GERMANY
    private val numberFormat = NumberFormat.getInstance(locale)

    private val creditDebitPattern = "\\d+(\\.\\d{3})*)+,\\d{2} [HS]"

    private val ibanAndBicRegex = Regex("IBAN: (?<IBAN>[A-Z]{2}\\d{2}( \\d{4}){4} \\d{2}) BIC: (?<BIC>[A-Z\\d]{11})")
    private val fromDateAndOldBalanceRegex = Regex(
        "alter Kontostand vom (?<day>\\d{2})\\.(?<month>\\d{2})\\.(?<year>\\d{4})\\s+" +
                "(?<balance>($creditDebitPattern)"
    )
    private val toDateAndNewBalanceRegex = Regex(
        "\\s*neuer Kontostand vom (?<day>\\d{2})\\.(?<month>\\d{2})\\.(?<year>\\d{4})\\s+" +
                "(?<balance>($creditDebitPattern)"
    )
    private val bookingItemStartRegex = Regex(
        "(?<postDay>\\d{2})\\.(?<postMonth>\\d{2})\\. (?<valueDay>\\d{2})\\.(?<valueMonth>\\d{2})\\. " +
                "(?<type>.+?)\\s+(?<amount>($creditDebitPattern)"
    )
    private val bookingItemIndentRegex = Regex("^\\s{14,15}[^\\s].*\$")

    private val nullValueRegex = Regex("(?<name>\\w+)=null")

    override fun isApplicable(statementFile: File): Boolean {
        val filename = statementFile.absolutePath

        val reader = runCatching {
            PdfReader(filename)
        }.getOrElse {
            return false
        }

        val producer = reader.info["Producer"] ?: return false
        return applicableProducers.any { producer.startsWith(it) }
    }

    override fun parseInternal(statementFile: File, options: Map<String, String>): Statement {
        val filename = statementFile.absolutePath
        val text = extractText(filename)

        val lines = text.trim().lines()
        val it = lines.listIterator()

        val state = ParsingState()
        val bookings = mutableListOf<BookingItem>()

        while (it.hasNext()) {
            var line = it.next()

            if (state.type != null) {
                val info = mutableListOf<String>()

                while (line.matches(bookingItemIndentRegex)) {
                    info += line.trimStart()
                    if (!it.hasNext()) break
                    line = it.next()
                }

                with(state) {
                    val fromDate = checkNotNull(from?.date)

                    val postDate = LocalDate.of(fromDate.year, checkNotNull(postMonth), checkNotNull(postDay))
                    if (postDate < fromDate) {
                        postDate.plusYears(1)
                    }

                    val valueDate = LocalDate.of(fromDate.year, checkNotNull(valueMonth), checkNotNull(valueDay))
                    if (valueDate < fromDate) {
                        valueDate.plusYears(1)
                    }

                    bookings += BookingItem(
                        postDate = postDate,
                        valueDate = valueDate,
                        info = info,
                        amount = checkNotNull(amount),
                        type = checkNotNull(type)
                    )

                    type = null
                }
            }

            whenMatch(line) {
                pattern(ibanAndBicRegex) {
                    state.accountId = groups["IBAN"]?.value
                    state.bankId = groups["BIC"]?.value
                }

                pattern(fromDateAndOldBalanceRegex) {
                    state.from = parseDateAndBalance()
                }

                pattern(toDateAndNewBalanceRegex) {
                    state.to = parseDateAndBalance()
                }

                pattern(bookingItemStartRegex) {
                    logger.debug { "Start of booking item found." }

                    state.postDay = groups["postDay"]?.value?.toIntOrNull()
                    state.postMonth = groups["postMonth"]?.value?.toIntOrNull()

                    state.valueDay = groups["valueDay"]?.value?.toIntOrNull()
                    state.valueMonth = groups["valueMonth"]?.value?.toIntOrNull()

                    state.amount = groups["amount"]?.value?.parseCreditDebitNumber()
                    state.type = groups["type"]?.value?.toBookingType()
                }
            }
        }

        val missingPropertyNames = nullValueRegex.findAll(state.toString()).mapNotNull { matchResult ->
            matchResult.groups["name"]?.value.takeUnless {
                // The "type" should be null to indicate that no booking item is currently being parsed.
                it == "type"
            }
        }

        if (missingPropertyNames.any()) {
            throw ParseException("The following properties were not found: ${missingPropertyNames.joinToString()}", 0)
        }

        return Statement(
            filename = statementFile.name,
            locale = locale,
            bankId = checkNotNull(state.bankId),
            accountId = checkNotNull(state.accountId),
            fromDate = checkNotNull(state.from?.date?.plusDays(1)),
            toDate = checkNotNull(state.to?.date),
            balanceOld = checkNotNull(state.from?.balance),
            balanceNew = checkNotNull(state.to?.balance),
            sumIn = bookings.sumOf { it.amount.coerceAtLeast(0f).toDouble() }.toFloat(),
            sumOut = bookings.sumOf { it.amount.coerceAtMost(0f).toDouble() }.toFloat(),
            bookings = bookings
        )
    }

    private fun extractText(filename: String): String {
        val reader = runCatching {
            PdfReader(filename)
        }.getOrElse {
            throw ParseException("Error opening file '$filename'.", 0)
        }

        val text = buildString {
            for (i in 1..reader.numberOfPages) {
                runCatching {
                    val pageText = PdfTextExtractor.getTextFromPage(reader, i)
                    append(pageText)
                }.onFailure {
                    throw ParseException("Error extracting text from page.", i)
                }

                // Ensure text from each page ends with a new-line to separate from the first line on the next page.
                append("\n")
            }
        }

        return text
    }

    private fun MatchResult.parseDateAndBalance(): Balancing {
        val result = Balancing()

        val day = groups["day"]?.value?.toIntOrNull()
        val month = groups["month"]?.value?.toIntOrNull()
        val year = groups["year"]?.value?.toIntOrNull()

        if (day != null && month != null && year != null) {
            result.date = LocalDate.of(year, month, day)
        } else {
            logger.warn { "Incomplete from date found." }
        }

        result.balance = groups["balance"]?.value?.parseCreditDebitNumber()

        return result
    }

    private fun String.parseCreditDebitNumber(): Float {
        val (number, sign) = split(' ')
        val value = numberFormat.parse(number).toFloat()
        return when (sign) {
            "H" -> value
            "S" -> -value
            else -> throw IllegalArgumentException("Unsupported number suffix in '$this'.")
        }
    }

    private fun String.toBookingType() =
        // TODO: Make this configurable like categories.
        when (this) {
            "Auszahlung girocard" -> BookingType.ATM
            "EURO-Überweisung" -> BookingType.PAYMENT
            "Lastschrift" -> BookingType.DEBIT
            else -> BookingType.UNKNOWN
        }
}
