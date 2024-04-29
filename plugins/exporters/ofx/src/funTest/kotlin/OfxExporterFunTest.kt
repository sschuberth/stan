package dev.schuberth.stan.plugins.exporters.ofx

import dev.schuberth.stan.AbstractTextExporterFunTest

import java.time.LocalDateTime

private val DTSERVER = LocalDateTime.now().format(OfxExporter.DATE_FORMATTER)

class OfxExporterFunTest : AbstractTextExporterFunTest(
    OfxExporter(),
    { replace(Regex("(<DTSERVER>)\\d+"), "$1$DTSERVER") }
)
