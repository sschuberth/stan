package com.github.sschuberth.stst;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingItem {
    public LocalDate date;
    public LocalDate valueDate;
    public List<String> info;
    public float amount;

    public BookingItem(LocalDate date, LocalDate valueDate, String info, float amount) {
        this.date = date;
        this.valueDate = valueDate;
        this.info = new ArrayList<>();
        this.info.add(info);
        this.amount = amount;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();

        s.append(date);
        s.append(", ");
        s.append(valueDate);
        s.append(", ");
        s.append(amount);
        s.append("\n");
        for (String line : info) {
            s.append(line);
            s.append("\n");
        }

        return s.toString();
    }
}
