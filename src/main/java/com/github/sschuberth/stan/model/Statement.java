package com.github.sschuberth.stan.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Statement implements Comparable<Statement> {
    public String filename;
    public Locale locale;
    public String bankId;
    public String accountId;
    public LocalDate fromDate;
    public LocalDate toDate;
    public float balanceOld;
    public float balanceNew;
    public float sumIn;
    public float sumOut;
    public List<BookingItem> bookings = new ArrayList<>();

    public Statement(String filename, Locale locale, String bankId, String accountId, LocalDate fromDate, LocalDate toDate, float balanceOld, float balanceNew, float sumIn, float sumOut, List<BookingItem> bookings) {
        this.filename = filename;
        this.locale = locale;
        this.bankId = bankId;
        this.accountId = accountId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.balanceOld = balanceOld;
        this.balanceNew = balanceNew;
        this.sumIn = sumIn;
        this.sumOut = sumOut;
        this.bookings = bookings;
    }

    @Override
    public int compareTo(Statement st) {
        return fromDate.compareTo(st.fromDate);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("[\n");

        Iterator<BookingItem> bookingItemIterator = bookings.iterator();
        while (bookingItemIterator.hasNext()) {
            BookingItem item = bookingItemIterator.next();
            result.append(item.toString().replaceAll("(?m)^", "  "));
            if (bookingItemIterator.hasNext()) {
                result.append(",");
            }
            result.append("\n");
        }

        result.append("]");

        return result.toString();
    }
}
