package dev.schuberth.stan.plugins.parsers.postbank

import dev.schuberth.stan.statementGoranBolsec
import dev.schuberth.stan.statementPetraPfiffig

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.io.File

class PostbankPdfParserFunTest : StringSpec({
    val parser = PostbankPdfParser()

    "Petra Pfiffig account statement is parsed correctly" {
        val statement = parser.parse(File("src/funTest/assets/PB_KAZ_KtoNr_9999999999_06-04-2017_1200.pdf"))

        statement shouldBe statementPetraPfiffig.second
    }

    "Goran Bolsec account statement is parsed correctly" {
        val statement = parser.parse(File("src/funTest/assets/317970916-PB-KAZ-KtoNr-0914083113-03-06-2016-0313.pdf"))

        statement shouldBe statementGoranBolsec.second
    }
})
