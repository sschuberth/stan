package com.github.sschuberth.stan.functionaltest

import com.github.sschuberth.stan.exporters.QifExporter
import com.github.sschuberth.stan.parsers.PostbankPDFParser

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

import java.io.File

class QifExporterTest : StringSpec({
    "Demokonto account statement is exported correctly" {
        val baseName = "PB_KAZ_KtoNr_9999999999_06-04-2017_1200"

        val expectedQif = readResource("/$baseName-expected.qif")

        val qif = File.createTempFile(baseName, "qif")
        val statement = PostbankPDFParser.parse("$baseName.pdf")
        QifExporter().write(statement, qif.path)
        val actualQif = qif.readText()

        qif.delete() shouldBe true
        actualQif shouldBe expectedQif
    }

    "Goran Bolsec account statement is exported correctly" {
        val baseName = "317970916-PB-KAZ-KtoNr-0914083113-03-06-2016-0313"

        val expectedQif = readResource("/$baseName-expected.qif")

        val qif = File.createTempFile(baseName, "qif")
        val statement = PostbankPDFParser.parse("$baseName.pdf")
        QifExporter().write(statement, qif.path)
        val actualQif = qif.readText()

        qif.delete() shouldBe true
        actualQif shouldBe expectedQif
    }
})
