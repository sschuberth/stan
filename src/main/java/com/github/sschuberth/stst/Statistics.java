package com.github.sschuberth.stst;

import javafx.application.Application;
import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static java.nio.file.Files.newDirectoryStream;

public class Statistics extends Application {
    private static final String[] MONTH_NAMES = new DateFormatSymbols(Locale.US).getShortMonths();

    private class MyStackedBarChart<X, Y> extends BarChart<X, Y> {
        MyStackedBarChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
            super(xAxis, yAxis);
        }

        @Override
        public ObservableList<Node> getPlotChildren() {
            return super.getPlotChildren();
        }
    }

    private MyStackedBarChart<String, Number> chart = null;

    private final XYChart.Series<String, Number> balance = new XYChart.Series<>();
    private final XYChart.Series<String, Number> incoming = new XYChart.Series<>();
    private final XYChart.Series<String, Number> outgoing = new XYChart.Series<>();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Statement Statistics");

        List<Statement> statements = loadStatements();

        LocalDate firstStatementFromDate = statements.get(0).fromDate;
        LocalDate lastStatementToDate = statements.get(statements.size() - 1).toDate;
        Period period = firstStatementFromDate.until(lastStatementToDate);

        int statementsTotalMonths = (int) period.toTotalMonths();
        List<String> statementsMonthNames = new ArrayList<>(statementsTotalMonths);
        for (int i = 0; i < statementsTotalMonths; ++i) {
            statementsMonthNames.add(MONTH_NAMES[(firstStatementFromDate.getMonthValue() + i) % 12]);
        }

        balance.setName("Balance");
        for (int i = 0; i < statementsTotalMonths; ++i) {
            balance.getData().add(new XYChart.Data<>(statementsMonthNames.get(i), statements.get(i).balanceOld));
        }

        float upperBound = 0;
        incoming.setName("Incoming");
        for (int i = 0; i < statementsTotalMonths; ++i) {
            float value = statements.get(i).sumIn;
            incoming.getData().add(new XYChart.Data<>(statementsMonthNames.get(i), value));
            value += statements.get(i).balanceOld;
            if (value > upperBound) {
                upperBound = value;
            }
        }

        float lowerBound = 0;
        outgoing.setName("Outgoing");
        for (int i = 0; i < statementsTotalMonths; ++i) {
            float value = statements.get(i).sumOut;
            outgoing.getData().add(new XYChart.Data<>(statementsMonthNames.get(i), value));

            value = statements.get(i).balanceOld;
            if (value < lowerBound) {
                lowerBound = value;
            }

            value += statements.get(i).sumIn + statements.get(i).sumOut;
            if (value < lowerBound) {
                lowerBound = value;
            }
        }

        float range = upperBound - lowerBound;
        float margin = range * 0.1f;
        float tickUnit = roundTo(range * 0.2f, 5000);

        CategoryAxis xAxis = new CategoryAxis(FXCollections.observableArrayList(statementsMonthNames));
        xAxis.setLabel("Month");

        NumberAxis yAxis = new NumberAxis("€", roundTo(lowerBound - margin, 1000), roundTo(upperBound + margin, 1000), tickUnit);

        chart = new MyStackedBarChart<>(xAxis, yAxis);
        chart.setTitle("Monthly Payments");
        chart.getData().addAll(balance, incoming, outgoing);

        Scene scene = new Scene(chart, 800, 600);
        stage.setScene(scene);
        stage.show();

        // Apply an offset along the y-axis to all bars of the second series. For details see
        // http://stackoverflow.com/a/41904147/1127485.
        int dataSize = balance.getData().size();
        for (int i = 0; i < dataSize; ++i) {
            Node balanceBar = chart.getPlotChildren().get(i);
            Node incomingBar = chart.getPlotChildren().get(dataSize + i);
            Node outgoingBar = chart.getPlotChildren().get(dataSize * 2 + i);

            double balanceBarHeight = balanceBar.getLayoutBounds().getHeight();
            double incomingBarTranslate = statements.get(i).balanceOld < 0 ? balanceBarHeight : -balanceBarHeight;
            incomingBar.setTranslateY(incomingBarTranslate);

            double incomingBarHeight = incomingBar.getLayoutBounds().getHeight();
            outgoingBar.setTranslateY(incomingBarTranslate - incomingBarHeight - 1);
        }
    }

    private List<Statement> loadStatements() {
        List<Statement> statements = new ArrayList<>();

        for (String arg : getParameters().getRaw()) {
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

        Collections.sort(statements);

        Iterator<Statement> it = statements.iterator();
        if (!it.hasNext()) {
            System.err.println("No statements found.");
            System.exit(1);
        }

        Statement curr = it.next(), next;
        while (it.hasNext()) {
            next = it.next();

            if (!curr.toDate.plusDays(1).equals(next.fromDate)) {
                System.err.println("Statements '" + curr.filename + "' and '" + next.filename + "' are not consecutive.");
                System.exit(1);
            }

            if (curr.balanceNew != next.balanceOld) {
                System.err.println("Balances of statements '" + curr.filename + "' and '" + next.filename + "' are not consistent.");
                System.exit(1);
            }

            curr = next;
        }

        //it = statements.iterator();
        //while (it.hasNext()) {
        //    for (BookingItem item : it.next().bookings) {
        //        System.out.println(item);
        //    }
        //}

        return statements;
    }

    private long roundTo(double i, long v) {
        return Math.round(i / v) * v;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
