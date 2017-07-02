package com.github.sschuberth.stan.exporters;

import com.github.sschuberth.stan.model.Statement;

import java.io.IOException;

public interface Exporter {
    void write(Statement st, String filename) throws IOException;
}
