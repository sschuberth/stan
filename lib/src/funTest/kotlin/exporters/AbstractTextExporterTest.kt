package dev.schuberth.stan.exporters

import dev.schuberth.stan.Exporter
import dev.schuberth.stan.statementGoranBolsec
import dev.schuberth.stan.statementPetraPfiffig

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets

abstract class AbstractTextExporterTest(exporter: Exporter, transform: String.() -> String = { this }) : StringSpec({
    listOf(
        statementGoranBolsec to "317970916-PB-KAZ-KtoNr-0914083113-03-06-2016-0313",
        statementPetraPfiffig to "PB_KAZ_KtoNr_9999999999_06-04-2017_1200"
    ).forEach { (holderToStatement, filename) ->
        "${holderToStatement.first} statement is exported correctly" {
            val expected = File("src/funTest/assets/$filename-expected.${exporter.extension}")
                .readText()
                .transform()

            val actual = ByteArrayOutputStream().also { exporter.write(holderToStatement.second, it) }
                .toString(StandardCharsets.UTF_8)
                .transform()

            actual shouldBe expected
        }
    }
})
