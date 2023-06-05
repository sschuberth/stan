package dev.schuberth.stan.exporters

class Ofx1ExporterTest : AbstractTextExporterTest(
    Ofx1Exporter(),
    { replace(Regex("(<DTSERVER>)\\d+"), "\\1") }
)
