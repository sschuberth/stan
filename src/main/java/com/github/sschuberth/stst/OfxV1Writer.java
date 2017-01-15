package com.github.sschuberth.stst;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

public enum OfxV1Writer {
    OFX,

    ACCTID,
    ACCTTYPE,
    BALAMT,
    BANKACCTFROM,
    BANKID,
    BANKMSGSRSV1,
    BANKTRANLIST,
    CODE,
    CURDEF,
    DTASOF,
    DTEND,
    DTPOSTED,
    DTSERVER,
    DTSTART,
    LANGUAGE,
    LEDGERBAL,
    SEVERITY,
    SIGNONMSGSRSV1,
    SONRS,
    STATUS,
    STMTRS,
    STMTTRN,
    STMTTRNRS,
    TRNAMT,
    TRNTYPE,
    TRNUID;

    @Override
    public String toString() {
        return name().replace('_', '.');
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final String[] HEADER = {
            "OFXHEADER:100",
            "DATA:OFXSGML",
            "VERSION:160",
            "SECURITY:NONE",
            "ENCODING:UTF-8",
            "CHARSET:NONE",
            "COMPRESSION:NONE",
            "OLDFILEUID:NONE",
            "NEWFILEUID:NONE"
    };

    private static final int INDENTATION_SIZE = 4;

    private static Writer writer;
    private static StringBuilder indentation = new StringBuilder(INDENTATION_SIZE * 5);

    public void write(Statement st, String name, Locale locale) throws IOException {
        write(st, name, locale, Currency.getInstance(locale));
    }

    public void write(Statement st, String name, Locale locale, Currency currency) throws IOException {
        // It usually is bad practice to write a static field from an instance method, but the convention for this class
        // is to only use the enum's "OFX" instance.
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(name), StandardCharsets.UTF_8));

        writer.write(String.join("\n", HEADER) + "\n\n");

        final String fromDateStr = st.fromDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        final String toDateStr = st.toDate.format(DateTimeFormatter.BASIC_ISO_DATE);

        OFX.begin();
            SIGNONMSGSRSV1.begin();
                SONRS.begin();
                    writeStatusAggregate(0, "INFO");
                    DTSERVER.data(LocalDateTime.now().format(FORMATTER));
                    LANGUAGE.data(locale.getISO3Language().toUpperCase());
                SONRS.end();
            SIGNONMSGSRSV1.end();

            BANKMSGSRSV1.begin();
                STMTTRNRS.begin();
                    TRNUID.data(0);
                    writeStatusAggregate(0, "INFO");
                    STMTRS.begin();
                        CURDEF.data(currency.toString());
                        BANKACCTFROM.begin();
                            BANKID.data(st.bankId);
                            ACCTID.data(st.accountId);
                            ACCTTYPE.data("CHECKING");
                        BANKACCTFROM.end();
                        BANKTRANLIST.begin();
                            DTSTART.data(fromDateStr);
                            DTEND.data(toDateStr);
                            for (BookingItem item : st.bookings) {
                                writeStatementTransAction(item);
                            }
                        BANKTRANLIST.end();
                        LEDGERBAL.begin();
                            BALAMT.data(st.balance);
                            DTASOF.data(toDateStr);
                        LEDGERBAL.end();
                    STMTRS.end();
                STMTTRNRS.end();
            BANKMSGSRSV1.end();
        OFX.end();

        writer.close();
    }

    private void writeStatusAggregate(int code, String severity) throws IOException {
        STATUS.begin();
            CODE.data(code);
            SEVERITY.data(severity);
        STATUS.end();
    }

    private void writeStatementTransAction(BookingItem item) throws IOException {
        STMTTRN.begin();
            TRNTYPE.data(item.amount > 0 ? "CREDIT" : "DEBIT");
            DTPOSTED.data(item.postDate.format(DateTimeFormatter.BASIC_ISO_DATE));
            TRNAMT.data(item.amount);
        STMTTRN.end();
    }

    private void begin() throws IOException {
        writer.write(indentation + "<" + toString() + ">\n");
        for (int i = 0; i < INDENTATION_SIZE; ++i) {
            indentation.append(" ");
        }
    }

    private void end() throws IOException {
        indentation.delete(0, INDENTATION_SIZE);
        writer.write(indentation + "</" + toString() + ">\n");
    }

    private void data(String value) throws IOException {
        writer.write(indentation + "<" + toString() + ">" + value + "\n");
    }

    private void data(int value) throws IOException {
        data(String.valueOf(value));
    }

    private void data(float value) throws IOException {
        data(String.valueOf(value));
    }
}
