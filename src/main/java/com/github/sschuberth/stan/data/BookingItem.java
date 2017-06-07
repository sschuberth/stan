package com.github.sschuberth.stan.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
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
        Iterator<String> infoIterator = info.iterator();
        while (infoIterator.hasNext()) {
            s.append("\t\t");

            // Quote the info line.
            s.append("\"");
            s.append(infoIterator.next());
            s.append("\"");

            if (infoIterator.hasNext()) {
                s.append(",");
            }

            s.append("\n");
        }
        s.append("\t]\n");

        s.append("}");

        return s.toString();
    }
}
