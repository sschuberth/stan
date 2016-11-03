package com.github.sschuberth.stst;

import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;

import java.io.IOException;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

class PostbankPDFParser {
    private static class MyLocationTextExtractionStrategy extends LocationTextExtractionStrategy {
        private float spaceCharWidthFactor;

        MyLocationTextExtractionStrategy(float spaceCharWidthFactor) {
            super();
            this.spaceCharWidthFactor = spaceCharWidthFactor;
        }

        protected boolean isChunkAtWordBoundary(TextChunk chunk, TextChunk previousChunk) {
            float width = chunk.getLocation().getCharSpaceWidth();
            if (width < 0.1f) {
                return false;
            }

            float dist = chunk.getLocation().distParallelStart() - previousChunk.getLocation().distParallelEnd();
            return dist < -width || dist > width * spaceCharWidthFactor;
        }
    }

    public static void main(String[] args) {
        String name = "C:\\Users\\sebastian\\Downloads\\PB_KAZ_KtoNr_0680330308_06-10-2016_0105.pdf";
        StringBuilder text = new StringBuilder();

        try {
            PdfReader reader = new PdfReader(name);
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
                MyLocationTextExtractionStrategy strategy = new MyLocationTextExtractionStrategy(0.3f);
                text.append(PdfTextExtractor.getTextFromPage(reader, i, strategy));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print(text.toString());
    }
}
