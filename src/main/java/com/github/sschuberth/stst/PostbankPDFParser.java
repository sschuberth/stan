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

        BookingItem item = null;
        List<BookingItem> items = new ArrayList<>();

        Iterator<String> i = lines.iterator();
        while (i.hasNext()) {
            String line = i.next();

            if (!foundStart) {
                if (!line.equals(BOOKING_TABLE_HEADER)) {
                    continue;
                } else {
                    foundStart = true;
                    continue;
                }
            } else if (line.equals(BOOKING_PAGE_HEADER)) {
                i.next();
                foundStart = false;
                continue;
            }

            Matcher m = BOOKING_ITEM_PATTERN.matcher(line);
            if (m.matches()) {
                // TODO: Do not assume year 2016.
                LocalDate date = LocalDate.parse(m.group(1) + "2016", BOOKING_DATE_FORMATTER);
                LocalDate valueDate = LocalDate.parse(m.group(2) + "2016", BOOKING_DATE_FORMATTER);

                DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
                DecimalFormat format = new DecimalFormat("+ 0,000.#;- 0,000.#", symbols);

                String amountStr = m.group(4);
                float amount = format.parse(amountStr).floatValue();

                item = new BookingItem(date, valueDate, m.group(3), amount);
                items.add(item);
            } else {
                if (item != null) {
                    item.info.add(line);
                }
            }
        }

        return items;
    }
}
