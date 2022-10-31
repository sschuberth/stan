package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.parsers.PostbankPdfParser
import dev.schuberth.stan.utils.JSON

import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe

import java.io.File
import java.io.FileOutputStream

import kotlinx.serialization.encodeToString

class JsonExporterTest : StringSpec({
    val parser = PostbankPdfParser()

    "Demokonto account statement is exported correctly" {
        val baseName = "PB_KAZ_KtoNr_9999999999_06-04-2017_1200"

        val expectedText = File("src/funTest/assets/$baseName-expected.json").readText()
        val expectedJson = JSON.encodeToString(JSON.parseToJsonElement(expectedText))

        val jsonFile = tempfile(suffix = ".json")
        val statement = parser.parse(File("src/funTest/assets/$baseName.pdf"))
        JsonExporter().write(statement, FileOutputStream(jsonFile.path))
        val actualJson = jsonFile.readText()

        actualJson shouldBe expectedJson
        statement.bookings.none { it.type == BookingType.UNKNOWN } shouldBe true
    }

    "Goran Bolsec account statement is exported correctly" {
        val baseName = "317970916-PB-KAZ-KtoNr-0914083113-03-06-2016-0313"

        val expectedText = File("src/funTest/assets/$baseName-expected.json").readText()
        val expectedJson = JSON.encodeToString(JSON.parseToJsonElement(expectedText))

        val jsonFile = tempfile(suffix = ".json")
        val statement = parser.parse(File("src/funTest/assets/$baseName.pdf"))
        JsonExporter().write(statement, FileOutputStream(jsonFile.path))
        val actualJson = jsonFile.readText()

        actualJson shouldBe expectedJson
        statement.bookings.none { it.type == BookingType.UNKNOWN } shouldBe true
    }
})
