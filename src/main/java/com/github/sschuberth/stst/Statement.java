package com.github.sschuberth.stst;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Statement implements Comparable<Statement> {
    public String filename;
    public String bankId;
    public String accountId;
    public LocalDate fromDate;
    public LocalDate toDate;
    public List<BookingItem> bookings = new ArrayList<>();
    public float balance;

    public Statement(String filename, String bankId, String accountId, LocalDate fromDate, LocalDate toDate, List<BookingItem> bookings, float balance) {
        this.filename = filename;
        this.bankId = bankId;
        this.accountId = accountId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.bookings = bookings;
        this.balance = balance;
    }

    @Override
    public int compareTo(Statement st) {
        return fromDate.compareTo(st.fromDate);
    }
}
