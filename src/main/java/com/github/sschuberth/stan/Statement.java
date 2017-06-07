package com.github.sschuberth.stan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Statement implements Comparable<Statement> {
    public String filename;
    public String bankId;
    public String accountId;
    public LocalDate fromDate;
    public LocalDate toDate;
    public float balanceOld;
    public float balanceNew;
    public float sumIn;
    public float sumOut;
    public List<BookingItem> bookings = new ArrayList<>();

    public Statement(String filename, String bankId, String accountId, LocalDate fromDate, LocalDate toDate, float balanceOld, float balanceNew, float sumIn, float sumOut, List<BookingItem> bookings) {
        this.filename = filename;
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
}
