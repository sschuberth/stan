package dev.schuberth.stan.plugins.exporters.ofx

import dev.schuberth.stan.AbstractTextExporterFunTest

class OfxExporterFunTest : AbstractTextExporterFunTest(
    OfxExporter(),
    { replace(Regex("(<DTSERVER>)\\d+"), "\\1") }
)
