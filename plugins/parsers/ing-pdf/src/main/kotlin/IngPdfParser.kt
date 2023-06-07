package dev.schuberth.stan.plugins.parsers.ing

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor

import dev.schuberth.stan.Parser
import dev.schuberth.stan.model.BookingItem
import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.utils.Logger
import dev.schuberth.stan.utils.whenMatch
import dev.schuberth.stan.utils.withoutSuffix

import java.io.File
import java.text.NumberFormat
import java.text.ParseException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class IngPdfParser : Logger, Parser() {
    private data class ParsingState(
        var balanceOld: Float? = null,
        var balanceNew: Float? = null,
        var accountId: String? = null,
        var bankId: String? = null,
        var fromDate: LocalDate? = null,
        var toDate: LocalDate? = null,
        var item: BookingItem? = null
    )

    override val name = "INGPDF"

    private val applicableProducers = setOf(
        "A2WServer V1.0 r26, MHT PDFLib  V2.1 r9, Solaris 2.6-8 or GCC",
        "AFP2web SDK V3.1, MHT PDFLib  V3.0, Solaris 2.6-8 or GCC",
        "Maas PDF Library V3.0",
        "Maas PDF Library V3.1"
    )

    private val locale = Locale.GERMANY

    private val numberFormat = NumberFormat.getInstance(locale)

    private val statementDateFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    private val bookingItemDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", locale)

    private val datePattern = "\\d{2}\\.\\d{2}\\.\\d{4}"
    private val amountPattern = "(-?\\d+(\\.\\d{3})*)+,\\d{2}"

    private val bookingItemStartRegex = Regex("^(?<date>$datePattern) (?<info>.+) (?<amount>$amountPattern)")
    private val bookingItemSecondLineRegex = Regex("^(?<date>$datePattern)( (?<info>.+))?")

    override fun isApplicable(statementFile: File): Boolean {
        val filename = statementFile.absolutePath

        val reader = runCatching {
            PdfReader(filename)
        }.getOrElse {
            return false
        }

        val producer = reader.info["Producer"] ?: return false
        return producer in applicableProducers
    }

    override fun parseInternal(statementFile: File, options: Map<String, String>): Statement {
        val filename = statementFile.absolutePath
        val text = extractText(filename)

        val lines = text.trim().lines()
        val it = lines.listIterator()

        val state = ParsingState()
        val bookings = mutableListOf<BookingItem>()

        while (it.hasNext()) {
            val line = it.next()

            whenMatch(line) {
                "Alter Saldo " {
                    state.balanceOld = withoutSuffix(" Euro")?.let {
                        numberFormat.parse(it).toFloat()
                    }
                }

                "Neuer Saldo " {
                    if (state.balanceNew != null) {
                        // Finalize parsing of any previous item by adding it to the bookings.
                        bookings += checkNotNull(state.item)

                        // Consume all remaining lines after the second and final new balance to end parsing.
                        while (it.hasNext()) it.next()
                    } else {
                        state.balanceNew = withoutSuffix(" Euro")?.let {
                            numberFormat.parse(it).toFloat()
                        }
                    }
                }

                "IBAN " {
                    state.accountId = replace(" ", "")
                }

                "BIC " {
                    state.bankId = this
                }

                "Kontoauszug " {
                    if (state.fromDate != null) {
                        logger.debug { "Skipping date '$this' as it is already set to '${state.fromDate}'." }
                    } else {
                        val monthAndYear = YearMonth.parse(this, statementDateFormatter)

                        state.fromDate = monthAndYear.atDay(1)
                        state.toDate = monthAndYear.atEndOfMonth()
                    }
                }

                bookingItemStartRegex {
                    state.item?.run {
                        // Finalize parsing of any previous item by adding it to the bookings.
                        bookings += this
                        state.item = null
                    }

                    groups["info"]?.value?.run {
                        val type = substringBefore(' ')
                        val firstInfo = substringAfter(' ')

                        logger.debug { "Booking item of type '$type' starts at '$firstInfo'." }

                        state.item = BookingItem(
                            postDate = LocalDate.parse(groups["date"]?.value, bookingItemDateFormatter),
                            valueDate = LocalDate.EPOCH,
                            info = mutableListOf(firstInfo),
                            amount = numberFormat.parse(groups["amount"]?.value).toFloat(),
                            type = type.toBookingType()
                        )
                    }
                }

                bookingItemSecondLineRegex {
                    val item = state.item
                    if (item != null && item.valueDate == LocalDate.EPOCH) {
                        val secondInfo = groups["info"]?.value
                        if (secondInfo != null) {
                            item.info += secondInfo
                        }

                        state.item = item.copy(
                            valueDate = LocalDate.parse(groups["date"]?.value, bookingItemDateFormatter)
                        )
                    }
                }

                otherwise {
                    val item = state.item
                    if (item != null && item.valueDate != LocalDate.EPOCH) {
                        item.info += line
                    }
                }
            }
        }

        return Statement.EMPTY.copy(
            filename = statementFile.name,
            locale = locale,
            bankId = checkNotNull(state.bankId),
            accountId = checkNotNull(state.accountId),
            fromDate = checkNotNull(state.fromDate),
            toDate = checkNotNull(state.toDate),
            balanceOld = checkNotNull(state.balanceOld),
            balanceNew = checkNotNull(state.balanceNew),
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

    private fun String.toBookingType() =
        // TODO: Make this configurable like categories.
        when (this) {
            "Abbuchung" -> BookingType.DEBIT
            "Abschluss" -> BookingType.UNKNOWN
            "Entgelt" -> BookingType.UNKNOWN
            "Gutschrift" -> BookingType.CREDIT
            "Gutschrift/Dauerauftrag" -> BookingType.REPEATPMT
            "Lastschrift" -> BookingType.PAYMENT
            "Ueberweisung" -> BookingType.TRANSFER
            else -> BookingType.UNKNOWN
        }
}
