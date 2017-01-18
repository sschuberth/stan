package com.github.sschuberth.stst;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.newDirectoryStream;

class Main {
    public static void main(String[] args) {
        List<Statement> statements = new ArrayList<>();

        for (String arg : args) {
            File file = new File(arg);

            try (DirectoryStream<Path> stream = newDirectoryStream(file.getParentFile().toPath(), file.getName())) {
                stream.forEach(filename -> {
                    try {
                        Statement st = PostbankPDFParser.parse(filename.toString());
                        System.out.println("Successfully parsed statement '" + file.getName() + "' dated from " + st.fromDate + " to " + st.toDate + ".");
                        statements.add(st);
                    } catch (ParseException e) {
                        System.err.println("Error parsing '" + filename + "'.");
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                System.err.println("Error opening '" + arg + "'.");
            }
        }
    }
}
