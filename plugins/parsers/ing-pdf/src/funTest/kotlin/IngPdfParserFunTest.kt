package dev.schuberth.stan.plugins.parsers.ing

import dev.schuberth.stan.model.BookingType

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

import java.io.File
import java.time.LocalDate

class IngPdfParserFunTest : StringSpec({
    val parser = IngPdfParser()

    "2022-01-04 account statement is parsed correctly" {
        val statement = parser.parse(File("src/funTest/assets/Girokonto_5434805604_Kontoauszug_20220104.pdf"))

        assertSoftly {
            with(statement) {
                bankId shouldBe "INGDDEFFXXX"
                accountId shouldBe "DE93500105175434805604"
                fromDate shouldBe LocalDate.of(2021, 12, 1)
                toDate shouldBe LocalDate.of(2021, 12, 31)
                balanceOld shouldBe -683.68f
                balanceNew shouldBe -717.36f
                sumIn shouldBe 183.68f
                sumOut shouldBe -217.36f

                bookings shouldHaveSize 3

                with(bookings[0]) {
                    postDate shouldBe LocalDate.of(2021, 12, 7)
                    valueDate shouldBe LocalDate.of(2021, 12, 7)
                    info shouldBe listOf(
                        "RANDI IMAD ZAKARIA",
                        "Referenz: ZV0100323804198500000002"
                    )
                    amount shouldBe 183.68f
                    type shouldBe BookingType.CREDIT
                    category should beNull()
                }

                with(bookings[1]) {
                    postDate shouldBe LocalDate.of(2021, 12, 9)
                    valueDate shouldBe LocalDate.of(2021, 12, 9)
                    info shouldBe listOf(
                        "SPARKASSE MAINZ",
                        "MAINZ010//SPARKASSE MAINZ/DE 2021-1 2-08T17:57:00",
                        "Folgenr.000 Verfalld. 2024-12 Entgelt 5,90EUR",
                        "Mandat: 175720",
                        "Referenz: 00002223460928081221175700"
                    )
                    amount shouldBe -205.9f
                    type shouldBe BookingType.PAYMENT
                    category should beNull()
                }

                with(bookings[2]) {
                    postDate shouldBe LocalDate.of(2021, 12, 30)
                    valueDate shouldBe LocalDate.of(2021, 12, 30)
                    info shouldBe listOf(
                        "Abschluss"
                    )
                    amount shouldBe -11.46f
                    type shouldBe BookingType.UNKNOWN
                    category should beNull()
                }
            }
        }
    }

    "2022-03-02 account statement is parsed correctly" {
        val statement = parser.parse(File("src/funTest/assets/Girokonto_5434805604_Kontoauszug_20220302.pdf"))

        with(statement) {
            assertSoftly {
                bankId shouldBe "INGDDEFFXXX"
                accountId shouldBe "DE93500105175434805604"
                fromDate shouldBe LocalDate.of(2022, 2, 1)
                toDate shouldBe LocalDate.of(2022, 2, 28)
                balanceOld shouldBe -717.36f
                balanceNew shouldBe -705.9f
                sumIn shouldBe 217.36f
                sumOut shouldBe -205.9f

                bookings shouldHaveSize 2

                with(bookings[0]) {
                    postDate shouldBe LocalDate.of(2022, 2, 14)
                    valueDate shouldBe LocalDate.of(2022, 2, 14)
                    info shouldBe listOf(
                        "RANDI IMAD ZAKARIA",
                        "Referenz: ZV0100327964878800000002"
                    )
                    amount shouldBe 217.36f
                    type shouldBe BookingType.CREDIT
                    category should beNull()
                }

                with(bookings[1]) {
                    postDate shouldBe LocalDate.of(2022, 2, 28)
                    valueDate shouldBe LocalDate.of(2022, 2, 28)
                    info shouldBe listOf(
                        "RHEINHESSEN SPARKASSE",
                        "HINDE017//RHEINHESSEN SPARKASSE/DE 2022-02-26T12:55:31",
                        "Folgenr.000 Ver falld.2024-12 Entgelt 5,90EUR",
                        "Mandat: 125551",
                        "Referenz: 00002216215754260222125531"
                    )
                    amount shouldBe -205.9f
                    type shouldBe BookingType.PAYMENT
                    category should beNull()
                }
            }
        }
    }
})
