package dev.schuberth.stan.plugins.parsers.postbankdb

import dev.schuberth.stan.Parser
import dev.schuberth.stan.model.BookingItem
import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.utils.Logger
import dev.schuberth.stan.utils.whenMatch

import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.cos.COSObject
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDResources
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject
import org.apache.pdfbox.text.PDFTextStripper

class PostbankDbPdfParser : Parser(), Logger {
    private val applicableProducers = setOf(
        "XEP 4.28.759"
    )

    override val name = "PostbankDBPDF"

    override fun isApplicable(statementFile: File): Boolean {
        val document = runCatching {
            PDDocument.load(statementFile)
        }.getOrElse {
            return false
        }

        return document.use { it.documentInformation.producer } in applicableProducers
    }

    override fun parseInternal(statementFile: File, options: Map<String, String>): Statement {
        val text = PDDocument.load(statementFile).use { document ->
            // Ignore the ToUnicode tables of non-embedded fonts to fix garbled text being extracted, see
            // https://stackoverflow.com/a/45922162/1127485.
            for (i in 0 until document.numberOfPages) {
                val page = document.getPage(i)
                removeToUnicodeMaps(page.resources)
            }

            val stripper = PDFTextStripper()

            // This adds missing spaces between words.
            stripper.sortByPosition = true

            stripper.getText(document)
        }

        val data = ParsingData()
        val bookings = mutableListOf<BookingItem>()

        val i = text.lines().listIterator()
        while (i.hasNext()) {
            val line = i.next()

            whenMatch(line) {
                "BIC (SWIFT) " {
                    val bicLine = i.hasNext().takeIf { it }?.let { i.next() }
                    data.bankId = bicLine?.substringBefore(' ')?.removeSuffix("XXX")
                }

                "Auszug Seite von IBAN Alter Saldo per " {
                    val ibanAndBalanceLine = i.hasNext().takeIf { it }?.let { i.next() }

                    if (ibanAndBalanceLine != null) {
                        var iban = ""
                        ibanAndBalanceLine.split(' ').drop(3).takeWhile { part ->
                            (iban.length < 22).also { if (it) iban += part }
                        }
                        data.accountId = iban

                        val balance = ibanAndBalanceLine.substringAfterLast(" EUR ")
                        data.balanceOld = bookingFormat.parse(balance).toFloat()
                    }
                }

                statementDatesRegex {
                    data.fromDate = groups["from"]?.value?.let { LocalDate.parse(it, dateFormatter) }
                    data.toDate = groups["to"]?.value?.let { LocalDate.parse(it, dateFormatter) }
                }

                "Buchung Valuta Vorgang Soll Haben" {
                    if (data.state == ParsingState.INITIAL) data.state = ParsingState.BOOKINGS
                }

                if (data.state == ParsingState.BOOKINGS) {
                    bookingStartRegex {
                        // Flush the current booking item.
                        data.bookingItem?.also { bookings += it }

                        val yearsLine = i.hasNext().takeIf { it }?.let { i.next() } ?: return@bookingStartRegex

                        val parts = yearsLine.split(' ', limit = 3)
                        val postDate = checkNotNull(groups["postDate"]).value + parts.getOrElse(0) { "" }
                        val valueDate = checkNotNull(groups["valueDate"]).value + parts.getOrElse(1) { "" }
                        val infoStart = checkNotNull(groups["info"]).value
                        val amount = checkNotNull(groups["amount"]).value

                        val info = buildList {
                            add(infoStart)

                            if (parts.size > 2) add(parts[2])

                            while (i.hasNext()) {
                                val nextLine = i.next()

                                if (bookingStartRegex.matches(nextLine) || nextLine == BALANCING_START) {
                                    i.previous()
                                    break
                                }

                                add(nextLine)
                            }
                        }

                        // Create a new booking item.
                        val item = BookingItem(
                            postDate = LocalDate.parse(postDate, dateFormatter),
                            valueDate = LocalDate.parse(valueDate, dateFormatter),
                            info = info.toMutableList(),
                            amount = bookingFormat.parse(amount).toFloat(),
                            // TODO: Implement type mapping by looking at "infoStart".
                            type = BookingType.UNKNOWN
                        )

                        logger.debug { "Created booking item dated ${item.postDate} for ${item.amount}." }

                        data.bookingItem = item
                    }
                }

                BALANCING_START {
                    if (data.state == ParsingState.BOOKINGS) {
                        data.state = ParsingState.BALANCING

                        val balanceLine = i.hasNext().takeIf { it }?.let { i.next() }

                        if (balanceLine != null) {
                            val balance = balanceLine.substringAfterLast(" EUR ")
                            data.balanceNew = bookingFormat.parse(balance).toFloat()
                        }
                    }
                }
            }
        }

        // Flush the last booking item.
        data.bookingItem?.also { bookings += it }

        // Accoount for a special booking due to the Postbank to DB IT migration.
        if (data.balanceOld == 0.0f) {
            val info = bookings.first().joinInfo(" ")
            if (BOOKING_FOR_IT_MIGRATION_NOTICE in info) {
                val migrationBooking = bookings.removeFirst()
                data.fromDate = migrationBooking.postDate.plusDays(1)
                data.balanceOld = migrationBooking.amount
            }
        }

        val amounts = bookings.map { it.amount.toDouble() }
        val (sumIn, sumOut) = amounts.partition { it >= 0 }.toList().map { it.sum().toFloat() }

        return Statement.EMPTY.copy(
            filename = statementFile.name,
            locale = Locale.GERMANY,
            bankId = checkNotNull(data.bankId),
            accountId = checkNotNull(data.accountId),
            fromDate = checkNotNull(data.fromDate),
            toDate = checkNotNull(data.toDate),
            balanceOld = checkNotNull(data.balanceOld),
            balanceNew = checkNotNull(data.balanceNew),
            sumIn = sumIn,
            sumOut = sumOut,
            bookings = bookings
        )
    }
}

private const val DATE_PATTERN = "\\d{2}\\.\\d{2}\\.\\d{4}"
private val statementDatesRegex = Regex("^Kontoauszug vom (?<from>$DATE_PATTERN) bis (?<to>$DATE_PATTERN)$")

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY)

private val bookingSymbols = DecimalFormatSymbols(Locale.GERMAN)
private val bookingFormat = DecimalFormat("+ 0,000.#;- 0,000.#", bookingSymbols)

private const val BOOKING_DATE_PATTERN = "\\d{2}\\.\\d{2}\\."
private const val BOOKING_AMOUNT_PATTERN = "[+-] [\\d.,]+"
private val bookingStartRegex = Regex(
    "^(?<postDate>$BOOKING_DATE_PATTERN) (?<valueDate>$BOOKING_DATE_PATTERN) " +
        "(?<info>.+) " +
        "(?<amount>$BOOKING_AMOUNT_PATTERN)$"
)

private const val BOOKING_FOR_IT_MIGRATION_NOTICE = "Die Buchung hat technische Gruende: Wir stellen das IT-System " +
        "fuer alle Konten um. Ihr Kontostand vor und nach den Buchungen bleibt gleich."

private const val BALANCING_START = "Filialnummer Kontonummer Neuer Saldo"

private enum class ParsingState {
    INITIAL,
    BOOKINGS,
    BALANCING
}

private data class ParsingData(
    var state: ParsingState = ParsingState.INITIAL,

    var bankId: String? = null,
    var accountId: String? = null,
    var fromDate: LocalDate? = null,
    var toDate: LocalDate? = null,
    var balanceOld: Float? = null,
    var balanceNew: Float? = null,

    var bookingItem: BookingItem? = null
)

private fun removeToUnicodeMaps(pdResources: PDResources) {
    val resources = pdResources.cosObject
    val fonts = resources.getDictionaryObject(COSName.FONT) as? COSDictionary

    fonts?.values?.forEach {
        var entry = it
        while (entry is COSObject) entry = entry.getObject()

        if (entry is COSDictionary) {
            entry.removeItem(COSName.TO_UNICODE)
        }
    }

    pdResources.xObjectNames.forEach {
        val obj = pdResources.getXObject(it)
        if (obj is PDFormXObject) {
            val xobjectPdResources = obj.resources
            removeToUnicodeMaps(xobjectPdResources)
        }
    }
}
