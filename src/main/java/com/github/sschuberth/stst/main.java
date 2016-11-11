package com.github.sschuberth.stst;

import java.text.ParseException;
import java.util.List;

class Main {
    public static void main(String[] args) {
        String filename = "C:\\Development\\Projects\\GitHub\\StatementStats\\src\\test\\resources\\PB_KAZ_KtoNr_0680330308_06-01-2016_0621.pdf";
        List<BookingItem> items = null;
        try {
            items = PostbankPDFParser.parse(filename);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        for (BookingItem item : items) {
            System.out.println(item);
        }
    }
}
