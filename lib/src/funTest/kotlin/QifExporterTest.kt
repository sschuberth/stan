package com.github.sschuberth.stan.functionaltest

import com.github.sschuberth.stan.exporters.QifExporter
import com.github.sschuberth.stan.parsers.PostbankPDFParser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.io.File
import java.io.FileOutputStream

class QifExporterTest : StringSpec({
    "Demokonto account statement is exported correctly" {
        val baseName = "PB_KAZ_KtoNr_9999999999_06-04-2017_1200"

        val expectedQif = File("src/funTest/assets/$baseName-expected.qif").readText()

        val qif = createTempFile(suffix = ".qif")
        val statement = PostbankPDFParser.parse(File("src/funTest/assets/$baseName.pdf"))
        QifExporter().write(statement, FileOutputStream(qif.path))
        val actualQif = qif.readText()

        qif.delete() shouldBe true
        actualQif shouldBe expectedQif
    }

    "Goran Bolsec account statement is exported correctly" {
        val baseName = "317970916-PB-KAZ-KtoNr-0914083113-03-06-2016-0313"

        val expectedQif = File("src/funTest/assets/$baseName-expected.qif").readText()

        val qif = createTempFile(suffix = ".qif")
        val statement = PostbankPDFParser.parse(File("src/funTest/assets/$baseName.pdf"))
        QifExporter().write(statement, FileOutputStream(qif.path))
        val actualQif = qif.readText()

        qif.delete() shouldBe true
        actualQif shouldBe expectedQif
    }
})
