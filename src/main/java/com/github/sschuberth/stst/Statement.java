package com.github.sschuberth.stst;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Statement {
    public String bankId;
    public String accountId;
    public LocalDate fromDate;
    public LocalDate toDate;
    public List<BookingItem> bookings = new ArrayList<>();
    public float balance;

    public Statement(String bankId, String accountId, LocalDate fromDate, LocalDate toDate, List<BookingItem> bookings, float balance) {
        this.bankId = bankId;
        this.accountId = accountId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.bookings = bookings;
        this.balance = balance;
    }
}
