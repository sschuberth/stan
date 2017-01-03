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
import java.util.Iterator;
import java.util.List;
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

    public static List<BookingItem> parse(String filename) throws ParseException {
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

                for (PdfName key : pageFonts.getKeys()) {
                    PdfDictionary fontDictionary = pageFonts.getAsDict(key);
                    fontDictionary.put(PdfName.TOUNICODE, null);
                }

                // For some reason we must not share the strategy across pages to get correct results.
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

                TextExtractionStrategy strategy = new FilteredTextRenderListener(new MyLocationTextExtractionStrategy(0.3f), filter);
                text.append(PdfTextExtractor.getTextFromPage(reader, i, strategy)).append("\n");
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

        Float sumIn = null, sumOut = null;

        Iterator<String> i = lines.iterator();
        while (i.hasNext()) {
            String line = i.next();

            if (!foundStart) {
                if (line.equals(BOOKING_TABLE_HEADER)) {
                    foundStart = true;
                }

                continue;
            }

            if (line.equals(BOOKING_PAGE_HEADER)) {
                i.next();
                foundStart = false;

                continue;
            } else if (line.equals(BOOKING_SUMMARY_IN)) {
                Matcher m = BOOKING_SUMMARY_PATTERN.matcher(i.next());
                if (m.matches()) {
                    String amountStr = m.group(2);
                    sumIn = bookingFormat.parse(amountStr).floatValue();
                }

                continue;
            } else if (line.equals(BOOKING_SUMMARY_OUT)) {
                Matcher m = BOOKING_SUMMARY_PATTERN.matcher(i.next());
                if (m.matches()) {
                    String amountStr = m.group(2);
                    sumOut = bookingFormat.parse(amountStr).floatValue();
                }

                break;
            }

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
                if (currentItem != null) {
                    currentItem.info.add(line);
                }
            }
        }

        if (sumIn == null || sumOut == null) {
            return null;
        }

        float calcIn = 0, calcOut = 0;
        for (BookingItem item : items) {
            if (item.amount > 0) {
                calcIn += item.amount;
            } else {
                calcOut += item.amount;
            }
        }

        if (Math.abs(calcIn - sumIn) >= 0.01 || Math.abs(calcOut - sumOut) >= 0.01) {
            return null;
        }

        return items;
    }
}
