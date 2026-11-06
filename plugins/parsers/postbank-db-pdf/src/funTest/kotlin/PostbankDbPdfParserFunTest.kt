package dev.schuberth.stan.plugins.parsers.postbankdb

import dev.schuberth.stan.model.BookingItem
import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.model.Statement

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.io.File
import java.time.LocalDate
import java.util.Locale

class PostbankDbPdfParserFunTest : StringSpec({
    val parser = PostbankDbPdfParser()

    "Business client sample account statement is parsed correctly" {
        val resource = checkNotNull(javaClass.getResource("/Postbank-Kontoauszug-Geschaeftskunden-Muster.pdf"))

        val statement = parser.parse(File(resource.toURI()))

        statement shouldBe Statement(
            filename = "Postbank-Kontoauszug-Geschaeftskunden-Muster.pdf",
            locale = Locale.Builder().setLanguage("de").setRegion("DE").build(),
            bankId = "DEUTDEDBPB2",
            accountId = "DE85215741240050026400",
            fromDate = LocalDate.of(2023, 5, 20),
            toDate = LocalDate.of(2023, 5, 23),
            balanceOld = 13297.16f,
            balanceNew = 2347.16f,
            sumIn = 8250.0f,
            sumOut = -19200.0f,
            bookings = listOf(
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 4, 20),
                    info = mutableListOf(
                        "SEPA Lastschrifteinzug bei",
                        "Marc-x Postbank",
                        "IBAN DE10215706230220030100",
                        "BIC NORSDE51XXX",
                        "Verwendungszweck/ Kundenreferenz",
                        "612/050026426 Postbank Dispofonds"
                    ),
                    amount = 100.0f,
                    type = BookingType.OTHER
                ),
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 5, 8),
                    info = mutableListOf(
                        "SEPA Lastschrifteinzug bei",
                        "Marc-x Postbank",
                        "IBAN DE10215706230220030100",
                        "BIC NORSDE51XXX",
                        "Verwendungszweck/ Kundenreferenz",
                        "612/050026425 Postbank Dispofonds"
                    ),
                    amount = 100.0f,
                    type = BookingType.OTHER
                ),
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 5, 22),
                    info = mutableListOf(
                        "Verwendungszweck/ Kundenreferenz",
                        "KAPITALTRANSAKTIONEN STK/NOM: 700",
                        "ANR.A.BIOCARTIS GROUP NV 2",
                        "WKNA3DZU5ISIN:BE0970181849 BELEGNR.",
                        "00000000000 IHRE REF: DEP 612/050026400"
                    ),
                    amount = 1050.0f,
                    type = BookingType.OTHER
                ),
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 4, 14),
                    info = mutableListOf(
                        "Verwendungszweck/ Kundenreferenz",
                        "ZINSEN/DIVIDENDEN/ERTRAEGE STK/NOM:",
                        "700 VOLKSWAGEN AG INHABER-STAMMAKTIEN",
                        "O.N."
                    ),
                    amount = 7000.0f,
                    type = BookingType.OTHER
                ),
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 5, 8),
                    info = mutableListOf(
                        "Verwendungszweck/ Kundenreferenz",
                        "WERTPAPIER-KAUF STK/NOM: 1,5611",
                        "DWS GLOBAL WATER INHABER-ANTEILE LD",
                        "BELEGNR: 1027215675"
                    ),
                    amount = -100.0f,
                    type = BookingType.OTHER
                ),
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 4, 20),
                    info = mutableListOf(
                        "Verwendungszweck/ Kundenreferenz",
                        "WERTPAPIER-KAUF STK/NOM: 1,5611",
                        "DWS GLOBAL WATER INHABER-ANTEILE LD",
                        "BELEGNR: 1027215631",
                        "0000000003 / 06500900 / 20230523"
                    ),
                    amount = -100.0f,
                    type = BookingType.OTHER
                ),
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 4, 20),
                    info = mutableListOf(
                        "Verwendungszweck/ Kundenreferenz",
                        "WERTPAPIER-KAUF STK/NOM: 59,4707",
                        "DWS GLOBAL WATER INHABER-ANTEILE LD",
                        "BELEGNR: 1027215637"
                    ),
                    amount = -4000.0f,
                    type = BookingType.OTHER
                ),
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 5, 8),
                    info = mutableListOf(
                        "Verwendungszweck/ Kundenreferenz",
                        "WERTPAPIER-KAUF STK/NOM: 59,4707",
                        "DWS GLOBAL WATER INHABER-ANTEILE LD",
                        "BELEGNR: 1027215671"
                    ),
                    amount = -4000.0f,
                    type = BookingType.OTHER
                ),
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 4, 20),
                    info = mutableListOf(
                        "Verwendungszweck/ Kundenreferenz",
                        "WERTPAPIER-KAUF STK/NOM: 37,7079",
                        "DWS GLOBAL WATER INHABER-ANTEILE LD",
                        "BELEGNR: 1027215642"
                    ),
                    amount = -2500.0f,
                    type = BookingType.OTHER
                ),
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 5, 8),
                    info = mutableListOf(
                        "Verwendungszweck/ Kundenreferenz",
                        "WERTPAPIER-KAUF STK/NOM: 37,7079",
                        "DWS GLOBAL WATER INHABER-ANTEILE LD",
                        "BELEGNR: 1027215672"
                    ),
                    amount = -2500.0f,
                    type = BookingType.OTHER
                ),
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 4, 14),
                    info = mutableListOf(
                        "Verwendungszweck/ Kundenreferenz",
                        "ZINSEN/DIVIDENDEN/ERTRAEGE STK/NOM:",
                        "500 VOLKSWAGEN AG INHABER-STAMMAKTIEN",
                        "O.N."
                    ),
                    amount = -5000.0f,
                    type = BookingType.OTHER
                ),
                BookingItem(
                    postDate = LocalDate.of(2023, 5, 23),
                    valueDate = LocalDate.of(2023, 4, 14),
                    info = mutableListOf(
                        "Verwendungszweck/ Kundenreferenz",
                        "STORNO ZINSEN/DIV/ERTRAEGE STK/NOM:",
                        "100 VOLKSWAGEN AG INHABER-STAMMAKTIEN",
                        "O.N."
                    ),
                    amount = -1000.0f,
                    type = BookingType.OTHER
                )
            )
        )
    }
})
