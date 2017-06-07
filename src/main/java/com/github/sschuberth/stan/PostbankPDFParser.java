package com.github.sschuberth.stan;

import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.FilteredTextRenderListener;
import com.itextpdf.text.pdf.parser.ImageRenderInfo;
import com.itextpdf.text.pdf.parser.LineSegment;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.RenderFilter;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextRenderInfo;
import com.itextpdf.text.pdf.parser.Vector;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PostbankPDFParser {
    private static Pattern STATEMENT_DATE_PATTERN = Pattern.compile("^Kontoauszug: (.+) vom (\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d) bis (\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d)$");
    private static DateTimeFormatter STATEMENT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static String BOOKING_PAGE_HEADER = "Auszug Seite IBAN BIC (SWIFT)";
    private static String BOOKING_PAGE_HEADER_BALANCE_OLD = "Alter Kontostand";
    private static String BOOKING_TABLE_HEADER = "Buchung Wert Vorgang/Buchungsinformation Soll Haben";
    private static Pattern BOOKING_ITEM_PATTERN = Pattern.compile("^(\\d\\d\\.\\d\\d\\.) (\\d\\d\\.\\d\\d\\.) (.+) ([+-] ?[\\d.,]+)$");
    private static Pattern BOOKING_ITEM_PATTERN_NO_SIGN = Pattern.compile("^(\\d\\d\\.\\d\\d\\.) (\\d\\d\\.\\d\\d\\.) (.+) ([\\d.,]+)$");

    private static String BOOKING_SUMMARY_IN = "Kontonummer BLZ Summe Zahlungseingänge";
    private static String BOOKING_SUMMARY_OUT = "Dispositionskredit Zinssatz für Dispositionskredit Summe Zahlungsausgänge";
    private static String BOOKING_SUMMARY_BALANCE_SINGULAR = "Zinssatz für geduldete Überziehung Anlage Neuer Kontostand";
    private static String BOOKING_SUMMARY_BALANCE_PLURAL = "Zinssatz für geduldete Überziehung Anlagen Neuer Kontostand";

    private static DecimalFormatSymbols BOOKING_SYMBOLS = new DecimalFormatSymbols(Locale.GERMAN);
    private static DecimalFormat BOOKING_FORMAT = new DecimalFormat("+ 0,000.#;- 0,000.#", BOOKING_SYMBOLS);
    private static Pattern BOOKING_SUMMARY_PATTERN = Pattern.compile("^(.*) EUR ([+-] [\\d.,]+)$");

    /*
     * Use an extraction strategy that allow to customize the ratio between the regular character width and the space
     * character width to tweak recognition of word boundaries. Inspired by http://stackoverflow.com/a/13645183/1127485.
     */
    private static class MyLocationTextExtractionStrategy extends LocationTextExtractionStrategy {
        private float spaceCharWidthFactor;

        MyLocationTextExtractionStrategy(float spaceCharWidthFactor) {
            super();
            this.spaceCharWidthFactor = spaceCharWidthFactor;
        }

        @Override
        protected boolean isChunkAtWordBoundary(TextChunk chunk, TextChunk previousChunk) {
            float width = chunk.getLocation().getCharSpaceWidth();
            if (width < 0.1f) {
                return false;
            }

            float dist = chunk.getLocation().distParallelStart() - previousChunk.getLocation().distParallelEnd();
            return dist < -width || dist > width * spaceCharWidthFactor;
        }
    }

    private static Float parseBookingSummary(String marker, String line, ListIterator<String> it) throws ParseException {
        do {
            marker = marker.replaceFirst(line + "\\s?", "");
            if (marker.isEmpty()) {
                break;
            }
            line = it.next();
        } while (marker.startsWith(line));

        line = it.next();
        Matcher m = BOOKING_SUMMARY_PATTERN.matcher(line);
        if (!m.matches()) {
            // Try appending the next line before we fail.
            line = line.trim() + " " + it.next().trim();
            m = BOOKING_SUMMARY_PATTERN.matcher(line);
            if (!m.matches()) {
                throw new ParseException("Error parsing booking summary", it.nextIndex());
            }
        }

        return BOOKING_FORMAT.parse(m.group(2)).floatValue();
    }

    public static Statement parse(String filename) throws ParseException {
        StringBuilder text = new StringBuilder();

        PdfReader reader;
        try {
            reader = new PdfReader(filename);
        } catch (IOException e) {
            throw new ParseException("Error opening file", 0);
        }

        for (int i = 1; i <= reader.getNumberOfPages(); ++i) {
            PdfDictionary pageResources = reader.getPageResources(i);
            if (pageResources == null) {
                continue;
            }

            PdfDictionary pageFonts = pageResources.getAsDict(PdfName.FONT);
            if (pageFonts == null) {
                continue;
            }

            // Ignore the ToUnicode tables of non-embedded fonts to fix garbled text being extracted, see
            // http://stackoverflow.com/a/37786643/1127485.
            for (PdfName key : pageFonts.getKeys()) {
                PdfDictionary fontDictionary = pageFonts.getAsDict(key);
                fontDictionary.put(PdfName.TOUNICODE, null);
            }

            // Create a filter to ignore vertical text.
            RenderFilter filter = new RenderFilter() {
                @Override
                public boolean allowText(TextRenderInfo renderInfo) {
                    LineSegment line = renderInfo.getBaseline();
                    return line.getStartPoint().get(Vector.I1) != line.getEndPoint().get(Vector.I1);
                }

                @Override
                public boolean allowImage(ImageRenderInfo renderInfo) {
                    return false;
                }
            };

            // For some reason we must not share the strategy across pages to get correct results.
            TextExtractionStrategy strategy = new FilteredTextRenderListener(new MyLocationTextExtractionStrategy(0.3f), filter);
            try {
                text.append(PdfTextExtractor.getTextFromPage(reader, i, strategy));
            } catch (IOException e) {
                throw new ParseException("Error extracting text from page", i);
            }

            // Ensure text from each page ends with a new-line to separate from the first line on the next page.
            text.append("\n");
        }

        List<String> lines = Arrays.asList(text.toString().split("\\n"));

        boolean foundStart = false;

        BookingItem currentItem = null;
        List<BookingItem> items = new ArrayList<>();

        LocalDate stFrom = null, stTo = null;
        int postYear = LocalDate.now().getYear(), valueYear = LocalDate.now().getYear();
        String accIban = null, accBic = null;
        Float sumIn = null, sumOut = null;
        Float balanceOld = null, balanceNew = null;

        String signLine = null;

        ListIterator<String> it = lines.listIterator();
        while (it.hasNext()) {
            String line = it.next();

            // Uncomment for debugging.
            //System.out.println(line);

            Matcher m = STATEMENT_DATE_PATTERN.matcher(line);
            if (m.matches()) {
                if (stFrom != null) {
                    throw new ParseException("Multiple statement start dates found", it.nextIndex());
                }
                stFrom = LocalDate.parse(m.group(2), STATEMENT_DATE_FORMATTER);

                if (stTo != null) {
                    throw new ParseException("Multiple statement end dates found", it.nextIndex());
                }
                stTo = LocalDate.parse(m.group(3), STATEMENT_DATE_FORMATTER);

                postYear = valueYear = stFrom.getYear();
            } else if (line.startsWith(BOOKING_PAGE_HEADER)) {
                // Read the IBAN and BIC from the page header.
                StringBuilder pageIban = new StringBuilder(22);

                String[] info = it.next().split(" ");

                if (info.length >= 9) {
                    if (accBic != null && !accBic.equals(info[8])) {
                        throw new ParseException("Inconsistent BIC", it.nextIndex());
                    }
                    accBic = info[8];

                    for (int i = 2; i < 8; ++i) {
                        pageIban.append(info[i]);
                    }

                    if (accIban != null && !accIban.equals(pageIban.toString())) {
                        throw new ParseException("Inconsistent IBAN", it.nextIndex());
                    }
                    accIban = pageIban.toString();
                }

                if (line.endsWith(BOOKING_PAGE_HEADER_BALANCE_OLD) && info.length == 12) {
                    // Work around a period being used instead of comma.
                    String signStr = info[10], amountStr = info[11];
                    char[] amountChars = amountStr.toCharArray();
                    int index = amountStr.length() - 3;
                    if (amountChars[index] == '.') {
                        amountChars[index] = ',';
                        amountStr = String.valueOf(amountChars);
                    }

                    balanceOld = BOOKING_FORMAT.parse(signStr + " " + amountStr).floatValue();
                }

                // Start looking for the table header again.
                foundStart = false;

                continue;
            } else if (BOOKING_SUMMARY_IN.startsWith(line)) {
                sumIn = parseBookingSummary(BOOKING_SUMMARY_IN, line, it);
                continue;
            } else if (BOOKING_SUMMARY_OUT.startsWith(line)) {
                sumOut = parseBookingSummary(BOOKING_SUMMARY_OUT, line, it);
                continue;
            } else if (BOOKING_SUMMARY_BALANCE_SINGULAR.startsWith(line)) {
                balanceNew = parseBookingSummary(BOOKING_SUMMARY_BALANCE_SINGULAR, line, it);

                // This is the last thing we are interested to parse, so break out of the loop early to avoid the need
                // to filter out coming unwanted stuff.
                break;
            } else if (BOOKING_SUMMARY_BALANCE_PLURAL.startsWith(line)) {
                balanceNew = parseBookingSummary(BOOKING_SUMMARY_BALANCE_PLURAL, line, it);

                // This is the last thing we are interested to parse, so break out of the loop early to avoid the need
                // to filter out coming unwanted stuff.
                break;
            } if (line.equals("+") || line.equals("-")) {
                signLine = line;
                continue;
            }

            // Loop until the booking table header is found, and then skip it.
            if (!foundStart) {
                if (line.equals(BOOKING_TABLE_HEADER)) {
                    foundStart = true;
                }

                continue;
            }

            // Work around the sign being present on the previous line.
            m = BOOKING_ITEM_PATTERN.matcher(line);
            if (!m.matches()) {
                m = BOOKING_ITEM_PATTERN_NO_SIGN.matcher(line);
                if (m.matches() && signLine != null) {
                    line = String.join(" ", m.group(1), m.group(2), m.group(3), signLine, m.group(4));
                    signLine = null;
                    m = BOOKING_ITEM_PATTERN.matcher(line);
                }
            }

            // Within the booking table, a matching pattern creates a new booking item.
            if (m.matches()) {
                LocalDate postDate = LocalDate.parse(m.group(1) + postYear, STATEMENT_DATE_FORMATTER);
                LocalDate valueDate = LocalDate.parse(m.group(2) + valueYear, STATEMENT_DATE_FORMATTER);

                if (currentItem != null) {
                    // If there is a wrap-around in the month, increase the year.
                    if (postDate.getMonth().getValue() < currentItem.postDate.getMonth().getValue()) {
                        postDate = postDate.withYear(++postYear);
                    }

                    if (valueDate.getMonth().getValue() < currentItem.valueDate.getMonth().getValue()) {
                        valueDate = valueDate.withYear(++valueYear);
                    }
                }

                String amountStr = m.group(4);

                // Work around a missing space before the amount.
                if (amountStr.charAt(1) != ' ') {
                    String[] a = amountStr.split("", 2);
                    amountStr = a[0] + " " + a[1];
                }

                float amount = BOOKING_FORMAT.parse(amountStr).floatValue();

                currentItem = new BookingItem(postDate, valueDate, m.group(3), amount);
                items.add(currentItem);
            } else {
                // Add the line as info to the current booking item, if any.
                if (currentItem != null) {
                    currentItem.info.add(line);
                }
            }
        }

        if (stFrom == null) {
            throw new ParseException("No statement start date found", it.nextIndex());
        }
        if (stTo == null) {
            throw new ParseException("No statement end date found", it.nextIndex());
        }

        if (accIban == null) {
            throw new ParseException("No IBAN found", it.nextIndex());
        }
        if (accBic == null) {
            throw new ParseException("No BIC found", it.nextIndex());
        }

        if (sumIn == null) {
            throw new ParseException("No incoming booking summary found", it.nextIndex());
        }
        if (sumOut == null) {
            throw new ParseException("No outgoing booking summary found", it.nextIndex());
        }

        if (balanceOld == null) {
            throw new ParseException("No old balance found", it.nextIndex());
        }
        if (balanceNew == null) {
            throw new ParseException("No new balance found", it.nextIndex());
        }

        float calcIn = 0, calcOut = 0;
        for (BookingItem item : items) {
            if (item.amount > 0) {
                calcIn += item.amount;
            } else {
                calcOut += item.amount;
            }
        }

        if (Math.abs(calcIn - sumIn) >= 0.01) {
            throw new ParseException("Sanity check on incoming booking summary failed", it.nextIndex());
        }

        if (Math.abs(calcOut - sumOut) >= 0.01) {
            throw new ParseException("Sanity check on outgoing booking summary failed", it.nextIndex());
        }

        float balanceCalc = balanceOld + sumIn + sumOut;
        if (Math.abs(balanceCalc - balanceNew) >= 0.01) {
            throw new ParseException("Sanity check on balances failed", it.nextIndex());
        }

        return new Statement(filename, accBic, accIban, stFrom, stTo, balanceOld, balanceNew, sumIn, sumOut, items);
    }
}
