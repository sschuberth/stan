package dev.schuberth.stan

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets

@Suppress("AbstractClassCanBeConcreteClass")
abstract class AbstractTextExporterFunTest(exporter: Exporter, transform: String.() -> String = { this }) : StringSpec({
    listOf(
        statementGoranBolsec to "PB_KAZ_KtoNr_0914083113_03-06-2016_0313",
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
