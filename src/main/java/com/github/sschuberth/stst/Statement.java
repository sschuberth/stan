package com.github.sschuberth.stst;

import java.util.ArrayList;
import java.util.List;

public class Statement {
    public String bankId;
    public String accountId;
    public List<BookingItem> bookings = new ArrayList<>();

    public Statement(String bankId, String accountId, List<BookingItem> bookings) {
        this.bankId = bankId;
        this.accountId = accountId;
        this.bookings = bookings;
    }
}
