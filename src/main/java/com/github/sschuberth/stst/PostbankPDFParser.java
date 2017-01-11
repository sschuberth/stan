package com.github.sschuberth.stst;

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
    private static String BOOKING_PAGE_HEADER = "Auszug Seite IBAN BIC (SWIFT)";
    private static String BOOKING_TABLE_HEADER = "Buchung Wert Vorgang/Buchungsinformation Soll Haben";
    private static Pattern BOOKING_ITEM_PATTERN = Pattern.compile("^(\\d\\d\\.\\d\\d\\.) (\\d\\d\\.\\d\\d\\.) (.+) ([\\+-] [\\d\\.,]+)$");

    private static DateTimeFormatter BOOKING_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static String BOOKING_SUMMARY_IN = "Kontonummer BLZ Summe Zahlungseingänge";
    private static String BOOKING_SUMMARY_OUT = "Dispositionskredit Zinssatz für Dispositionskredit Summe Zahlungsausgänge";
    private static Pattern BOOKING_SUMMARY_PATTERN = Pattern.compile("^(.*) EUR ([\\+-] [\\d\\.,]+)$");

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

    public static Statement parse(String filename) throws ParseException {
        StringBuilder text = new StringBuilder();

        try {
            PdfReader reader = new PdfReader(filename);
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
                text.append(PdfTextExtractor.getTextFromPage(reader, i, strategy));

                // Ensure text from each page ends with a new-line to separate from the first line on the next page.
                text.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> lines = Arrays.asList(text.toString().split("\\n"));

        boolean foundStart = false;

        BookingItem currentItem = null;
        List<BookingItem> items = new ArrayList<>();

        DecimalFormatSymbols bookingSymbols = new DecimalFormatSymbols(Locale.GERMAN);
        DecimalFormat bookingFormat = new DecimalFormat("+ 0,000.#;- 0,000.#", bookingSymbols);

        String accIban = null, accBic = null;
        Float sumIn = null, sumOut = null;

        ListIterator<String> it = lines.listIterator();
        while (it.hasNext()) {
            String line = it.next();

            // Loop until the booking table header is found, and then skip it.
            if (!foundStart) {
                if (line.equals(BOOKING_TABLE_HEADER)) {
                    foundStart = true;
                }

                continue;
            }

            if (line.equals(BOOKING_PAGE_HEADER)) {
                // Read the IBAN and BIC from the page header.
                StringBuilder pageIban = new StringBuilder(22);

                String[] info = it.next().split(" ");

                if (info.length == 9) {
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

                // Start looking for the table header again.
                foundStart = false;

                continue;
            } else if (BOOKING_SUMMARY_IN.startsWith(line)) {
                // Allow the incoming booking summary to span multiple lines.
                String in = BOOKING_SUMMARY_IN;
                do {
                    in = in.replaceFirst(line + "\\s?", "");
                    if (in.isEmpty()) {
                        break;
                    }
                    line = it.next();
                } while (in.startsWith(line));

                // Extract the incoming sum from the next line.
                Matcher m = BOOKING_SUMMARY_PATTERN.matcher(it.next());
                if (!m.matches()) {
                    throw new ParseException("Error parsing incoming booking summary", it.nextIndex());
                }

                String amountStr = m.group(2);
                sumIn = bookingFormat.parse(amountStr).floatValue();

                continue;
            } else if (BOOKING_SUMMARY_OUT.startsWith(line)) {
                // Allow the outgoing booking summary to span multiple lines.
                String out = BOOKING_SUMMARY_OUT;
                do {
                    out = out.replaceFirst(line + "\\s?", "");
                    if (out.isEmpty()) {
                        break;
                    }
                    line = it.next();
                } while (out.startsWith(line));

                // Extract the outgoing sum from the next line.
                Matcher m = BOOKING_SUMMARY_PATTERN.matcher(it.next());
                if (!m.matches()) {
                    throw new ParseException("Error parsing outgoing booking summary", it.nextIndex());
                }

                String amountStr = m.group(2);
                sumOut = bookingFormat.parse(amountStr).floatValue();

                break;
            }

            // A matching pattern creates a new booking item.
            Matcher m = BOOKING_ITEM_PATTERN.matcher(line);
            if (m.matches()) {
                // TODO: Do not assume year 2016.
                LocalDate date = LocalDate.parse(m.group(1) + "2016", BOOKING_DATE_FORMATTER);
                LocalDate valueDate = LocalDate.parse(m.group(2) + "2016", BOOKING_DATE_FORMATTER);

                String amountStr = m.group(4);
                float amount = bookingFormat.parse(amountStr).floatValue();

                currentItem = new BookingItem(date, valueDate, m.group(3), amount);
                items.add(currentItem);
            } else {
                // Add the line as info to the current booking item, if any.
                if (currentItem != null) {
                    currentItem.info.add(line);
                }
            }
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

        return new Statement(accBic, accIban, items);
    }
}
