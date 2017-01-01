package com.github.sschuberth.stst;

import java.text.ParseException;
import java.util.List;

class Main {
    public static void main(String[] args) {
        String filename = args[0];
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
