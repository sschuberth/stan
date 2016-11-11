package com.github.sschuberth.stst;

import java.time.LocalDate;

public class BookingItem {
    public LocalDate date;
    public LocalDate valueDate;
    public StringBuilder info;
    public float amount;

    public BookingItem(LocalDate date, LocalDate valueDate, String info, float amount) {
        this.date = date;
        this.valueDate = valueDate;
        this.info = new StringBuilder(info);
        this.amount = amount;
    }

    @Override
    public String toString() {
        return date + " " + valueDate + " " + info + " " + amount;
    }
}
