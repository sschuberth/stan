package com.github.sschuberth.stan.exporters;

import com.github.sschuberth.stan.model.Statement;

import java.io.IOException;
import java.io.PrintWriter;

public class JsonExporter implements Exporter {
    @Override
    public void write(Statement st, String filename) throws IOException {
        try (PrintWriter out = new PrintWriter(filename)) {
            out.print(st.toString());
        }
    }
}
