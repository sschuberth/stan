package dev.schuberth.stan

import java.io.PrintWriter
import java.io.Writer

class UnixPrintWriter(writer: Writer) : PrintWriter(writer) {
    // Always use Unix line endings instead of System.lineSeparator().
    override fun println() = write("\n")
}
