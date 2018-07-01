package com.github.sschuberth.stan.functionaltest

import com.github.salomonbrys.kotson.fromJson
import com.github.sschuberth.stan.exporters.JsonExporter
import com.github.sschuberth.stan.parsers.PostbankPDFParser

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

import java.io.File

class JsonExporterTest : StringSpec({
    val gson = GsonBuilder().setPrettyPrinting().create()

    "Demokonto account statement is exported correctly" {
        val baseName = "PB_KAZ_KtoNr_9999999999_06-04-2017_1200"

        val expectedText = readResource("/$baseName-expected.json")
        val expectedJson = gson.toJson(gson.fromJson<JsonArray>(expectedText))

        val json = File.createTempFile(baseName, "json")
        val statement = PostbankPDFParser.parse(File("src/funTest/resources/$baseName.pdf"))
        JsonExporter().write(statement, json.path)
        val actualJson = json.readText()

        json.delete() shouldBe true
        actualJson shouldBe expectedJson
    }

    "Goran Bolsec account statement is exported correctly" {
        val baseName = "317970916-PB-KAZ-KtoNr-0914083113-03-06-2016-0313"

        val expectedText = readResource("/$baseName-expected.json")
        val expectedJson = gson.toJson(gson.fromJson<JsonArray>(expectedText))

        val json = File.createTempFile(baseName, "json")
        val statement = PostbankPDFParser.parse(File("src/funTest/resources/$baseName.pdf"))
        JsonExporter().write(statement, json.path)
        val actualJson = json.readText()

        json.delete() shouldBe true
        actualJson shouldBe expectedJson
    }
})
