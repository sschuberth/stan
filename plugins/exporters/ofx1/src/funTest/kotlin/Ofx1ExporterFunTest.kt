package dev.schuberth.stan.plugins.exporters.ofx1

import dev.schuberth.stan.AbstractTextExporterFunTest

class Ofx1ExporterFunTest : AbstractTextExporterFunTest(
    Ofx1Exporter(),
    { replace(Regex("(<DTSERVER>)\\d+"), "\\1") }
)
