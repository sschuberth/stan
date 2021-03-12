package dev.schuberth.stan.parsers

import com.itextpdf.text.pdf.PdfName
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.FilteredTextRenderListener
import com.itextpdf.text.pdf.parser.ImageRenderInfo
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.RenderFilter
import com.itextpdf.text.pdf.parser.TextRenderInfo
import com.itextpdf.text.pdf.parser.Vector

import dev.schuberth.stan.model.BookingItem
import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.model.Configuration
import dev.schuberth.stan.model.Statement

import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.Locale

import kotlin.io.path.createTempDirectory
import kotlin.math.abs

private const val STATEMENT_BIC_HEADER_2017 = "BIC (SWIFT):"

private const val BOOKING_PAGE_HEADER_2014 = "Auszug Seite IBAN BIC (SWIFT)"
private const val BOOKING_PAGE_HEADER_2017 = "Auszug Jahr Seite von IBAN"
private const val BOOKING_PAGE_HEADER_BALANCE_OLD = "Alter Kontostand"

private const val BOOKING_SUMMARY_IN = "Kontonummer BLZ Summe Zahlungseingänge"
private const val BOOKING_SUMMARY_OUT = "Dispositionskredit Zinssatz für Dispositionskredit Summe Zahlungsausgänge"
private const val BOOKING_SUMMARY_OUT_ALT =
    "Eingeräumte Kontoüberziehung Zinssatz für eingeräumte Kontoüberziehung Summe Zahlungsausgänge"
private const val BOOKING_SUMMARY_BALANCE_SINGULAR = "Zinssatz für geduldete Überziehung Anlage Neuer Kontostand"
private const val BOOKING_SUMMARY_BALANCE_PLURAL = "Zinssatz für geduldete Überziehung Anlagen Neuer Kontostand"

/*
 * Use an extraction strategy that allow to customize the ratio between the regular character width and the space
 * character width to tweak recognition of word boundaries. Inspired by http://stackoverflow.com/a/13645183/1127485.
 */
private class MyLocationTextExtractionStrategy(
    private val spaceCharWidthFactor: Float
) : LocationTextExtractionStrategy() {
    override fun isChunkAtWordBoundary(chunk: TextChunk, previousChunk: TextChunk): Boolean {
        val width = chunk.location.charSpaceWidth
        if (width < 0.1f) {
            return false
        }

        val dist = chunk.location.distParallelStart() - previousChunk.location.distParallelEnd()
        return dist < -width || dist > width * spaceCharWidthFactor
    }
}

/*
 * A filter to ignore vertical text.
 */
private class VerticalTextFilter : RenderFilter() {
    override fun allowText(renderInfo: TextRenderInfo?): Boolean {
        val line = renderInfo!!.baseline
        return line.startPoint.get(Vector.I1) != line.endPoint.get(Vector.I1)
    }

    override fun allowImage(renderInfo: ImageRenderInfo?) = false
}

class PostbankPdfParser(config: Configuration) : Parser(config) {
    private val pdfDateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    private val statementDatePattern = Regex(
        "Kontoauszug: (.+) vom (\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d) bis (\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d)"
    )
    private val statementDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    private val bookingTableHeader = Regex("Buchung[ /]Wert Vorgang/Buchungsinformation Soll Haben")

    private val bookingItemPattern = Regex("(\\d\\d\\.\\d\\d\\.)[ /](\\d\\d\\.\\d\\d\\.) (.+) ([+-] ?[\\d.,]+)")
    private val bookingItemPatternNoSign = Regex("(\\d\\d\\.\\d\\d\\.)[ /](\\d\\d\\.\\d\\d\\.) (.+) ([\\d.,]+)")

    private val bookingSummaryPattern = Regex("(.*) ?(EUR) ([+-] [\\d.,]+)")

    private val bookingSymbols = DecimalFormatSymbols(Locale.GERMAN)
    private val bookingFormat = DecimalFormat("+ 0,000.#;- 0,000.#", bookingSymbols)

    private fun extractText(filename: String): Pair<String, Boolean> {
        val reader = runCatching {
            PdfReader(filename)
        }.getOrElse {
            throw ParseException("Error opening file '$filename'.", 0)
        }

        val pdfInfo = reader.info
        val pdfCreationDate = pdfInfo["CreationDate"]?.let { creationDate ->
            LocalDate.parse(creationDate.substring(2, 16), pdfDateFormatter).also {
                if (it.isBefore(LocalDate.of(2014, 7, 1))) {
                    throw ParseException("Unsupported statement format.", 0)
                }
            }
        }

        val isFormat2014 = pdfCreationDate?.isBefore(LocalDate.of(2017, 6, 1)) ?: false
        val text = buildString {
            for (i in 1..reader.numberOfPages) {
                val pageResources = reader.getPageResources(i) ?: continue
                val pageFonts = pageResources.getAsDict(PdfName.FONT) ?: continue

                if (isFormat2014) {
                    // Ignore the ToUnicode tables of non-embedded fonts to fix garbled text being extracted, see
                    // http://stackoverflow.com/a/37786643/1127485.
                    pageFonts.keys
                        .map { pageFonts.getAsDict(it) }
                        .forEach { it.put(PdfName.TOUNICODE, null) }
                }

                // For some reason we must not share the strategy across pages to get correct results.
                val strategy = MyLocationTextExtractionStrategy(0.3f)
                val listener = FilteredTextRenderListener(strategy, VerticalTextFilter())

                runCatching {
                    val pageText = PdfTextExtractor.getTextFromPage(reader, i, listener)
                    append(pageText)
                }.onFailure {
                    throw ParseException("Error extracting text from page.", i)
                }

                // Ensure text from each page ends with a new-line to separate from the first line on the next page.
                append("\n")
            }
        }

        return text to isFormat2014
    }

    private fun mapType(infoLine: String) =
        // TODO: Make this configurable like categories.
        when (infoLine) {
            "Auszahlung Geldautomat", "Bargeldausz. Geldautomat", "Kartenverfüg" -> BookingType.ATM

            "Auszahlung", "Bargeldauszahlung" -> BookingType.CASH

            "Scheckeinreichung", "Scheckeinr", "Inh. Scheck" -> BookingType.CHECK

            "Gutschrift", "Gutschr.SEPA", "Gutschr. SEPA", "Storno: SDD Lastschr", "paydirekt Rückzahlung",
            "Einzahlung", "Retoure" -> BookingType.CREDIT

            "Kartenlastschrift", "Lastschrift", "SDD Lastschr", "paydirekt Zahlung" -> BookingType.DEBIT

            "Zinsen/Entg." -> BookingType.INT

            "Überweisung giropay", "Kartenzahlung", "Geldkarte", "Gutscheinkauf" -> BookingType.PAYMENT

            "Gehalt/Rente" -> BookingType.SALARY

            "SEPA Überw. Einzel", "SEPA Überw. BZÜ", "Umbuchung" -> BookingType.TRANSFER

            else -> when (infoLine.split(' ').firstOrNull()) {
                "Gut" -> BookingType.CREDIT
                "Dauerauftrag" -> BookingType.REPEATPMT
                else -> BookingType.UNKNOWN
            }
        }

    private data class ParsingState(
        var foundStart: Boolean = false,

        val items: ArrayList<BookingItem> = arrayListOf(),

        var stFrom: LocalDate = LocalDate.EPOCH,
        var stTo: LocalDate = LocalDate.EPOCH,
        var accIban: String = "",
        var accBic: String = "",
        var sumIn: Float = Float.NaN,
        var sumOut: Float = Float.NaN,
        var balanceOld: Float = Float.NaN,
        var balanceNew: Float = Float.NaN,

        var signLine: String? = null
    )

    /**
     * Parse a single [item][BookingItem]. Returns `null` while parsing has not yet finished and needs to be called
     * again, or otherwise returns the final [state][ParsingState].
     */
    private fun parseItem(
        isFormat2014: Boolean,
        bookingPageHeader: String,
        state: ParsingState,
        it: ListIterator<String>
    ): ParsingState? {
        var line = it.next()

        var m = statementDatePattern.matchEntire(line)
        if (m != null) {
            if (state.stFrom != LocalDate.EPOCH) {
                throw ParseException("Multiple statement start dates found.", it.nextIndex())
            }
            state.stFrom = LocalDate.parse(m.groupValues[2], statementDateFormatter)

            if (state.stTo != LocalDate.EPOCH) {
                throw ParseException("Multiple statement end dates found.", it.nextIndex())
            }
            state.stTo = LocalDate.parse(m.groupValues[3], statementDateFormatter)
        } else if (!isFormat2014 && line.startsWith(STATEMENT_BIC_HEADER_2017)) {
            state.accBic = line.removePrefix(STATEMENT_BIC_HEADER_2017).trim()
        } else if (line.startsWith(bookingPageHeader) && it.hasNext()) {
            val info = it.next().split(" ").dropLastWhile { it.isBlank() }

            if (info.size >= 9) {
                // For the 2014 format only, read the BIC from the page header.
                if (isFormat2014) {
                    if (state.accBic.isNotEmpty() && state.accBic != info[8]) {
                        throw ParseException("Inconsistent BIC.", it.nextIndex())
                    }
                    state.accBic = info[8]
                }

                // Read the IBAN from the page header.
                val ibanOffset = if (isFormat2014) 2 else 4
                val pageIban = buildString {
                    repeat(6) {
                        append(info[ibanOffset + it])
                    }
                }

                if (state.accIban.isNotEmpty() && state.accIban != pageIban) {
                    throw ParseException("Inconsistent IBAN.", it.nextIndex())
                }

                state.accIban = pageIban
            }

            val oldBalanceOffset = if (isFormat2014) 10 else 11
            if (line.endsWith(BOOKING_PAGE_HEADER_BALANCE_OLD) && info.size == oldBalanceOffset + 2) {
                val signStr = info[oldBalanceOffset]
                var amountStr = info[oldBalanceOffset + 1]

                // Work around a period being used instead of comma.
                val amountChars = amountStr.toCharArray()
                val index = amountStr.length - 3
                if (amountChars[index] == '.') {
                    amountChars[index] = ','
                    amountStr = String(amountChars)
                }

                state.balanceOld = bookingFormat.parse("$signStr $amountStr").toFloat()
            }

            // Start looking for the table header again.
            state.foundStart = false

            return null
        } else if (BOOKING_SUMMARY_IN.startsWith(line)) {
            state.sumIn = parseSummary(BOOKING_SUMMARY_IN, line, it)
            return null
        } else if (BOOKING_SUMMARY_OUT.startsWith(line)) {
            state.sumOut = parseSummary(BOOKING_SUMMARY_OUT, line, it)
            return null
        } else if (BOOKING_SUMMARY_OUT_ALT.startsWith(line)) {
            state.sumOut = parseSummary(BOOKING_SUMMARY_OUT_ALT, line, it)
            return null
        } else if (BOOKING_SUMMARY_BALANCE_SINGULAR.startsWith(line)) {
            state.balanceNew = parseSummary(BOOKING_SUMMARY_BALANCE_SINGULAR, line, it)

            // This is the last thing we are interested to parse, so break out of the loop early to avoid the need
            // to filter out coming unwanted stuff.
            finalizeLastItem(state.items)
            return state
        } else if (BOOKING_SUMMARY_BALANCE_PLURAL.startsWith(line)) {
            state.balanceNew = parseSummary(BOOKING_SUMMARY_BALANCE_PLURAL, line, it)

            // This is the last thing we are interested to parse, so break out of the loop early to avoid the need
            // to filter out coming unwanted stuff.
            finalizeLastItem(state.items)
            return state
        }
        if (line == "+" || line == "-") {
            state.signLine = line
            return null
        }

        // Loop until the booking table header is found, and then skip it.
        if (!state.foundStart) {
            if (line.matches(bookingTableHeader)) {
                state.foundStart = true
            }

            return null
        }

        m = bookingItemPattern.matchEntire(line)

        if (m == null) {
            // Work around the sign being present on the previous line.
            m = bookingItemPatternNoSign.matchEntire(line)
            if (m != null && state.signLine != null) {
                line = listOf(m.groupValues[1], m.groupValues[2], m.groupValues[3], state.signLine, m.groupValues[4])
                    .joinToString(" ")
                state.signLine = null
                m = bookingItemPattern.matchEntire(line)
            }
        }

        // Within the booking table, a matching pattern creates a new booking item.
        if (m != null) {
            // Initialize the post / value years with the year the statement starts from.
            var postDate = LocalDate.parse(m.groupValues[1] + state.stFrom.year, statementDateFormatter)
            var valueDate = LocalDate.parse(m.groupValues[2] + state.stFrom.year, statementDateFormatter)

            // Find the correct years by checking the statement date range.
            for (year in state.stFrom.year..state.stTo.year) {
                val guessedPostDate = LocalDate.parse(m.groupValues[1] + year, statementDateFormatter)
                if (state.stFrom <= guessedPostDate && guessedPostDate <= state.stTo) {
                    postDate = guessedPostDate
                }

                val guessedValueDate = LocalDate.parse(m.groupValues[2] + year, statementDateFormatter)
                if (state.stFrom <= guessedValueDate && guessedValueDate <= state.stTo) {
                    valueDate = guessedValueDate
                }
            }

            state.items.lastOrNull()?.let { finalizeLastItem(state.items) }

            var amountStr = m.groupValues[4]

            // Work around a missing space before the amount.
            if (amountStr[1] != ' ') {
                amountStr = "${amountStr.take(1)} ${amountStr.drop(1)}"
            }

            val infoLine = m.groupValues[3]
            val amount = bookingFormat.parse(amountStr).toFloat()
            val type = mapType(infoLine)

            // The category is yet known as we do not have all info lines at this point.
            state.items += BookingItem(postDate, valueDate, mutableListOf(infoLine), amount, type)
        } else {
            // Add the line as info to the current booking item, if any.
            state.items.lastOrNull()?.info?.add(line)
        }

        return null
    }

    private fun finalizeLastItem(items: MutableList<BookingItem>) {
        val item = items.removeLast()
        items += item.copy(category = config.findBookingCategory(item)?.name)
    }

    private fun parseSummary(startMarker: String, startLine: String, it: ListIterator<String>): Float {
        var marker = startMarker
        var line = startLine

        // Allow the marker to span multiple lines by incrementally
        // removing the current line from the beginning of the marker.
        do {
            marker = marker.replaceFirst(Regex("^$line ?"), "")

            if (it.hasNext()) line = it.next()

            if (marker.isEmpty()) {
                // Full marker match, next line is the one we are interested in.
                break
            }

            // No match yet, take the next line into consideration.
        } while (marker.startsWith(line))

        var m = bookingSummaryPattern.matchEntire(line)
        if (m == null) {
            var parsedText = line

            // Try appending the next line before we fail.
            if (it.hasNext()) {
                parsedText = line.trim() + " " + it.next().trim()
                m = bookingSummaryPattern.matchEntire(parsedText)

                // Try prepending the previous line before we fail.
                if (m == null) {
                    parsedText = it.previous().trim() + " " + line.trim()
                    m = bookingSummaryPattern.matchEntire(parsedText)
                }
            }

            if (m == null) {
                throw ParseException("Error parsing booking summary from text '$parsedText'.", it.nextIndex())
            }
        }

        return bookingFormat.parse(m.groupValues[3]).toFloat()
    }

    override fun parse(statementFile: File, options: Map<String, String>): Statement {
        val filename = statementFile.absolutePath
        val (text, isFormat2014) = extractText(filename)

        if (options["text.output"]?.toBoolean() == true) {
            val tempDir = createTempDirectory().toFile()
            val textFile = tempDir.resolve("${statementFile.nameWithoutExtension}.txt")
            textFile.writeText(text)
            println("Wrote text output to '$textFile'.")
        }

        val lines = text.lines().dropLastWhile { it.isBlank() }

        val bookingPageHeader = if (isFormat2014) BOOKING_PAGE_HEADER_2014 else BOOKING_PAGE_HEADER_2017
        val state = ParsingState()
        val it = lines.listIterator()

        while (it.hasNext()) {
            if (parseItem(isFormat2014, bookingPageHeader, state, it) != null) {
                break
            }
        }

        if (state.stFrom == LocalDate.EPOCH) {
            throw ParseException("No statement start date found.", it.nextIndex())
        }
        if (state.stTo == LocalDate.EPOCH) {
            throw ParseException("No statement end date found.", it.nextIndex())
        }

        if (state.accIban.isEmpty()) {
            throw ParseException("No IBAN found.", it.nextIndex())
        }
        if (state.accBic.isEmpty()) {
            throw ParseException("No BIC found.", it.nextIndex())
        }

        if (state.sumIn.isNaN()) {
            throw ParseException("No incoming booking summary found.", it.nextIndex())
        }
        if (state.sumOut.isNaN()) {
            throw ParseException("No outgoing booking summary found.", it.nextIndex())
        }

        if (state.balanceOld.isNaN()) {
            throw ParseException("No old balance found.", it.nextIndex())
        }
        if (state.balanceNew.isNaN()) {
            throw ParseException("No new balance found.", it.nextIndex())
        }

        var calcIn = 0.0f
        var calcOut = 0.0f
        for (item in state.items) {
            if (item.amount > 0) {
                calcIn += item.amount
            } else {
                calcOut += item.amount
            }
        }

        if (abs(calcIn - state.sumIn) >= 0.01) {
            throw ParseException("Sanity check on incoming booking summary failed.", it.nextIndex())
        }

        if (abs(calcOut - state.sumOut) >= 0.01) {
            throw ParseException("Sanity check on outgoing booking summary failed.", it.nextIndex())
        }

        val balanceCalc = state.balanceOld + state.sumIn + state.sumOut
        if (abs(balanceCalc - state.balanceNew) >= 0.01) {
            throw ParseException("Sanity check on balances failed.", it.nextIndex())
        }

        return Statement(
            filename = statementFile.name,
            locale = Locale.GERMANY,
            bankId = state.accBic,
            accountId = state.accIban,
            fromDate = state.stFrom,
            toDate = state.stTo,
            balanceOld = state.balanceOld,
            balanceNew = state.balanceNew,
            sumIn = state.sumIn,
            sumOut = state.sumOut,
            bookings = state.items
        )
    }
}
