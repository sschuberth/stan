package dev.schuberth.stan.exporters

import dev.schuberth.stan.model.BookingType
import dev.schuberth.stan.parsers.PostbankPDFParser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.io.File
import java.io.FileOutputStream

import kotlinx.serialization.json.JsonElement

class JsonExporterTest : StringSpec({
    "Demokonto account statement is exported correctly" {
        val baseName = "PB_KAZ_KtoNr_9999999999_06-04-2017_1200"

        val expectedText = File("src/funTest/assets/$baseName-expected.json").readText()
        val expectedJson = JSON.stringify(JsonElement.serializer(), JSON.parseJson(expectedText))

        val jsonFile = createTempFile(suffix = ".json")
        val statement = PostbankPDFParser.parse(File("src/funTest/assets/$baseName.pdf"))
        JsonExporter().write(statement, FileOutputStream(jsonFile.path))
        val actualJson = jsonFile.readText()

        jsonFile.delete() shouldBe true
        actualJson shouldBe expectedJson
        statement.bookings.none { it.type == BookingType.UNKNOWN } shouldBe true
    }

    "Goran Bolsec account statement is exported correctly" {
        val baseName = "317970916-PB-KAZ-KtoNr-0914083113-03-06-2016-0313"

        val expectedText = File("src/funTest/assets/$baseName-expected.json").readText()
        val expectedJson = JSON.stringify(JsonElement.serializer(), JSON.parseJson(expectedText))

        val jsonFile = createTempFile(suffix = ".json")
        val statement = PostbankPDFParser.parse(File("src/funTest/assets/$baseName.pdf"))
        JsonExporter().write(statement, FileOutputStream(jsonFile.path))
        val actualJson = jsonFile.readText()

        jsonFile.delete() shouldBe true
        actualJson shouldBe expectedJson
        statement.bookings.none { it.type == BookingType.UNKNOWN } shouldBe true
    }
})
