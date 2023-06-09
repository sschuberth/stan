package dev.schuberth.stan.plugins.exporters.ofx

import dev.schuberth.stan.AbstractTextExporterFunTest

import java.time.LocalDateTime

class OfxExporterFunTest : AbstractTextExporterFunTest(
    OfxExporter(),
    { replace(Regex("(<DTSERVER>)\\d+"), "$1${LocalDateTime.now().format(OfxExporter.DATE_FORMATTER)}") }
)
