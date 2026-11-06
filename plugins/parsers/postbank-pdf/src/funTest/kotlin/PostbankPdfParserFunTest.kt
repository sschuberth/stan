package dev.schuberth.stan.plugins.parsers.postbank

import dev.schuberth.stan.statementGoranBolsec
import dev.schuberth.stan.statementPetraPfiffig

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.io.File

class PostbankPdfParserFunTest : StringSpec({
    val parser = PostbankPdfParser()

    "Petra Pfiffig account statement is parsed correctly" {
        val resource = checkNotNull(javaClass.getResource("/PB_KAZ_KtoNr_9999999999_06-04-2017_1200.pdf"))

        val statement = parser.parse(File(resource.toURI()))

        statement shouldBe statementPetraPfiffig.second
    }

    "Goran Bolsec account statement is parsed correctly" {
        val resource = checkNotNull(javaClass.getResource("/PB_KAZ_KtoNr_0914083113_03-06-2016_0313.pdf"))

        val statement = parser.parse(File(resource.toURI()))

        statement shouldBe statementGoranBolsec.second
    }
})
