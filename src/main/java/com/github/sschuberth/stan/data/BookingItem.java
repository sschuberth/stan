package com.github.sschuberth.stan.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingItem {
    public LocalDate postDate;
    public LocalDate valueDate;
    public List<String> info;
    public float amount;

    public BookingItem(LocalDate postDate, LocalDate valueDate, String info, float amount) {
        this.postDate = postDate;
        this.valueDate = valueDate;
        this.info = new ArrayList<>();
        this.info.add(info);
        this.amount = amount;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append("{\n");

        s.append("\t\"postDate\": \"").append(postDate).append("\",\n");
        s.append("\t\"valueDate\": \"").append(valueDate).append("\",\n");
        s.append("\t\"amount\": \"").append(amount).append("\",\n");

        s.append("\t\"info\": [\n");
        for (String line : info) {
            s.append("\t\t\"");
            s.append(line);
            s.append("\",\n");
        }
        s.append("\t],\n");

        s.append("},\n");

        return s.toString();
    }
}
