package dev.schuberth.stan.utils

import kotlinx.serialization.json.Json

val JSON = Json {
    encodeDefaults = false
    prettyPrint = true
    prettyPrintIndent = "  "
}
