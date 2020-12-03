package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.Configuration
import dev.schuberth.stan.parsers.PostbankPdfParser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.io.File
import java.io.FileOutputStream

import kotlin.io.path.createTempFile

class OfxV1ExporterTest : StringSpec({
    val parser = PostbankPdfParser(Configuration.EMPTY)

    "Demokonto account statement is exported correctly" {
        val baseName = "PB_KAZ_KtoNr_9999999999_06-04-2017_1200"

        val expectedOfx = File("src/funTest/assets/$baseName-expected.ofx")
            .readText()
            .replace(Regex("(<DTSERVER>)\\d+"), "\\1")

        val ofx = createTempFile(suffix = ".ofx").toFile()
        val statement = parser.parse(File("src/funTest/assets/$baseName.pdf"))
        OfxV1Exporter().write(statement, FileOutputStream(ofx.path))
        val actualOfx = ofx.readText().replace(Regex("(<DTSERVER>)\\d+"), "\\1")

        ofx.delete() shouldBe true
        actualOfx shouldBe expectedOfx
    }

    "Goran Bolsec account statement is exported correctly" {
        val baseName = "317970916-PB-KAZ-KtoNr-0914083113-03-06-2016-0313"

        val expectedOfx = File("src/funTest/assets/$baseName-expected.ofx")
            .readText()
            .replace(Regex("(<DTSERVER>)\\d+"), "\\1")

        val ofx = createTempFile(suffix = ".ofx").toFile()
        val statement = parser.parse(File("src/funTest/assets/$baseName.pdf"))
        OfxV1Exporter().write(statement, FileOutputStream(ofx.path))
        val actualOfx = ofx.readText().replace(Regex("(<DTSERVER>)\\d+"), "\\1")

        ofx.delete() shouldBe true
        actualOfx shouldBe expectedOfx
    }
})
