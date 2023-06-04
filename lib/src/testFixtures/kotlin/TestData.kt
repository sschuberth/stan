package dev.schuberth.stan

import dev.schuberth.stan.model.BookingItem
import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.model.Statement

import java.time.LocalDate
import java.util.Locale

val statementGoranBolsec = "Goran Bolsec" to Statement(
    filename = "317970916-PB-KAZ-KtoNr-0914083113-03-06-2016-0313.pdf",
    locale = Locale.Builder().setLanguage("de").setRegion("DE").build(),
    bankId = "PBNKDEFF",
    accountId = "DE94100100100914083113",
    fromDate = LocalDate.of(2016, 4, 2),
    toDate = LocalDate.of(2016, 6, 2),
    balanceOld = -9.9f,
    balanceNew = 58.1f,
    sumIn = 968.0f,
    sumOut = -900.0f,
    bookings = listOf(
        BookingItem(
            postDate = LocalDate.of(2016, 5, 27),
            valueDate = LocalDate.of(2016, 5, 27),
            info = mutableListOf(
                "Gutschr.SEPA",
                "GORAN BOLSEC Verwendungszweck INTERNAL",
                "TRANSFER SALLARY GORAN BOLSEC"
            ),
            amount = 968.0f,
            type = BookingType.CREDIT,
            category = "Gehalt"
        ),
        BookingItem(
            postDate = LocalDate.of(2016, 5, 30),
            valueDate = LocalDate.of(2016, 5, 30),
            info = mutableListOf(
                "Auszahlung Geldautomat",
                "DEUTSCHE BANK Referenz",
                "03204718088636270516113921 Mandat 969148 Einrei-",
                "cher-ID DE7600200000132558 HUECKELHOV//Hückel-",
                "hoven/DE Terminal 03204718 2016-05-27T11:39:21 Fol-",
                "genr. 000 Verfalld. 2012"
            ),
            amount = -900.0f,
            type = BookingType.ATM,
            category = "Sonstige Ausgaben"
        )
    )
)

val statementPetraPfiffig = "Petra Pfiffig" to Statement(
    filename = "PB_KAZ_KtoNr_9999999999_06-04-2017_1200.pdf",
    locale = Locale.Builder().setLanguage("de").setRegion("DE").build(),
    bankId = "PBNKDEFF",
    accountId = "DE31200100209999999999",
    fromDate = LocalDate.of(2020, 1, 1),
    toDate = LocalDate.of(2020, 2, 2),
    balanceOld = 2853.82f,
    balanceNew = 5314.05f,
    sumIn = 4764.7f,
    sumOut = -2304.47f,
    bookings = listOf(
        BookingItem(
            postDate = LocalDate.of(2020, 1, 19),
            valueDate = LocalDate.of(2020, 1, 19),
            info = mutableListOf(
                "Gutschr. SEPA",
                "Referenz T1505626335515118F",
                "KINDERGELD-NR. 1462347",
                "ARBEITSAMT BONN"
            ),
            amount = 154.0f,
            type = BookingType.CREDIT,
            category = "Kindergeld"
        ),
        BookingItem(
            postDate = LocalDate.of(2020, 1, 19),
            valueDate = LocalDate.of(2020, 1, 19),
            info = mutableListOf(
                "SDD Lastschr",
                "Referenz C2234288433716048C ",
                "Mandat F24BUSI-20200305001",
                "STROMKOSTEN KD.NR.1462347",
                "JAHRESABRECHNUNG",
                "STADTWERKE MUSTERSTADT"
            ),
            amount = -580.06f,
            type = BookingType.DEBIT,
            category = "Betriebskosten"
        ),
        BookingItem(
            postDate = LocalDate.of(2020, 1, 21),
            valueDate = LocalDate.of(2020, 1, 21),
            info = mutableListOf(
                "SDD Lastschr",
                "Referenz I3634266643356018L ",
                "Mandat G55-20204665778",
                "TELEFON AG KÖLN"
            ),
            amount = -125.8f,
            type = BookingType.DEBIT,
            category = "Medien"
        ),
        BookingItem(
            postDate = LocalDate.of(2020, 1, 23),
            valueDate = LocalDate.of(2020, 1, 23),
            info = mutableListOf(
                "SDD Lastschr",
                "Referenz D593057869356017Z ",
                "Mandat UIL2099786858493",
                "TEILNEHMER 1234567",
                "RUNDFUNK 0103 - 1203",
                "GEZ"
            ),
            amount = -84.75f,
            type = BookingType.DEBIT,
            category = "Medien"
        ),
        BookingItem(
            postDate = LocalDate.of(2020, 1, 25),
            valueDate = LocalDate.of(2020, 1, 25),
            info = mutableListOf(
                "Inh. Scheck",
                "2000123456789"
            ),
            amount = -75.0f,
            type = BookingType.CHECK,
            category = "Sonstige Ausgaben"
        ),
        BookingItem(
            postDate = LocalDate.of(2020, 1, 25),
            valueDate = LocalDate.of(2020, 1, 25),
            info = mutableListOf(
                "SDD Lastschr",
                "Referenz 102030ZZ89446",
                "Mandat IO949596976",
                "MIETE 600 + 250 EUR  OBJ22/328",
                "SCHULSTR. 7, 12345 MEINHEIM",
                "EIGENHEIM KG",
                "Muster"
            ),
            amount = -850.0f,
            type = BookingType.DEBIT,
            category = "Miete"
        ),
        BookingItem(
            postDate = LocalDate.of(2020, 1, 25),
            valueDate = LocalDate.of(2020, 1, 25),
            info = mutableListOf(
                "Scheckeinreichung",
                "EINGANG VORBEHALTEN",
                "GUTBUCHUNG 12345",
                "EIN FREMDER"
            ),
            amount = 1830.0f,
            type = BookingType.CHECK,
            category = "Sonstige Einnahmen"
        ),
        BookingItem(
            postDate = LocalDate.of(2020, 1, 26),
            valueDate = LocalDate.of(2020, 1, 26),
            info = mutableListOf(
                "SEPA Überw. Einzel",
                "Rechnung Nr. DA1000001",
                "VERLAGSHAUS SCRIBERE GMBH"
            ),
            amount = -31.5f,
            type = BookingType.TRANSFER,
            category = "Sonstige Ausgaben"
        ),
        BookingItem(
            postDate = LocalDate.of(2020, 1, 28),
            valueDate = LocalDate.of(2020, 1, 28),
            info = mutableListOf(
                "Gutschr. SEPA",
                "Referenz 007009105597",
                "BEZÜGE PERS.NR. 70600170/01",
                "ARBEITGEBER U. CO",
                "PETRA PFIFFIG"
            ),
            amount = 2780.7f,
            type = BookingType.CREDIT,
            category = "Gehalt"
        ),
        BookingItem(
            postDate = LocalDate.of(2020, 1, 28),
            valueDate = LocalDate.of(2020, 1, 28),
            info = mutableListOf(
                "SEPA Überw. Einzel",
                "111111/3299999999/20010020",
                "ÜBERTRAG AUF SPARCARD",
                "3299999999",
                "PETRA PFIFFIG"
            ),
            amount = -228.61f,
            type = BookingType.TRANSFER,
            category = "Umbuchung"
        ),
        BookingItem(
            postDate = LocalDate.of(2020, 1, 29),
            valueDate = LocalDate.of(2020, 1, 29),
            info = mutableListOf(
                "SEPA Überw. Einzel",
                "111111/1000000000/37050198",
                "FINANZKASSE 3991234",
                "STEUERNUMMER 00703434",
                "FINANZKASSE KÖLN-SÜD"
            ),
            amount = -328.75f,
            type = BookingType.TRANSFER,
            category = "Steuer"
        )
    )
)
