package com.github.sschuberth.stan.functionaltest

import com.github.salomonbrys.kotson.fromJson
import com.github.sschuberth.stan.parsers.PostbankPDFParser

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class PostbankTest : StringSpec({
    val gson = GsonBuilder().setPrettyPrinting().create()

    "Demokonto account statement is parsed correctly" {
        val baseName = "PB_KAZ_KtoNr_9999999999_06-04-2017_1200"

        val expectedText = readResource("/$baseName-expected.json")
        val expectedJson = gson.toJson(gson.fromJson<JsonArray>(expectedText))

        val actualJson = PostbankPDFParser.parse("$baseName.pdf").toString()

        actualJson shouldBe expectedJson
    }

    "Goran Bolsec account statement is parsed correctly" {
        val baseName = "317970916-PB-KAZ-KtoNr-0914083113-03-06-2016-0313"

        val expectedText = readResource("/$baseName-expected.json")
        val expectedJson = gson.toJson(gson.fromJson<JsonArray>(expectedText))

        val actualJson = PostbankPDFParser.parse("$baseName.pdf").toString()

        actualJson shouldBe expectedJson
    }
})
