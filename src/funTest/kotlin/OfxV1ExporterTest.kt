package com.github.sschuberth.stan.functionaltest

import com.github.sschuberth.stan.exporters.OfxV1Exporter
import com.github.sschuberth.stan.parsers.PostbankPDFParser

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

import java.io.File

class OfxV1ExporterTest : StringSpec({
    "Demokonto account statement is exported correctly" {
        val baseName = "PB_KAZ_KtoNr_9999999999_06-04-2017_1200"

        val expectedOfx = readResource("/$baseName-expected.ofx")
                .replace(Regex("(<DTSERVER>)\\d+"), "\\1")

        val ofx = File.createTempFile(baseName, "ofx")
        val statement = PostbankPDFParser.parse(File("src/funTest/resources/$baseName.pdf"))
        OfxV1Exporter().write(statement, ofx.path)
        val actualOfx = ofx.readText().replace(Regex("(<DTSERVER>)\\d+"), "\\1")

        ofx.delete() shouldBe true
        actualOfx shouldBe expectedOfx
    }

    "Goran Bolsec account statement is exported correctly" {
        val baseName = "317970916-PB-KAZ-KtoNr-0914083113-03-06-2016-0313"

        val expectedOfx = readResource("/$baseName-expected.ofx")
                .replace(Regex("(<DTSERVER>)\\d+"), "\\1")

        val ofx = File.createTempFile(baseName, "ofx")
        val statement = PostbankPDFParser.parse(File("src/funTest/resources/$baseName.pdf"))
        OfxV1Exporter().write(statement, ofx.path)
        val actualOfx = ofx.readText().replace(Regex("(<DTSERVER>)\\d+"), "\\1")

        ofx.delete() shouldBe true
        actualOfx shouldBe expectedOfx
    }
})
