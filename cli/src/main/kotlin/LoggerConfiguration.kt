package dev.schuberth.stan.cli

import java.lang.System.Logger.Level

// The System.Logger API by default uses the java.util.logging.Logger implementation, so formatting needs to be
// customized via java.util.logging.* properties, see
// https://docs.oracle.com/cd/E57471_01/bigData.100/data_processing_bdd/src/rdp_logging_config.html
@Suppress("Unused", "UnusedPrivateProperty")
private val originalLogFormat = System.setProperty(
    "java.util.logging.SimpleFormatter.format",
    "%1\$tF %1\$tT.%1\$tL %4$-7s %3\$s - %5\$s%n"
)

private val rootLogger by lazy { java.util.logging.LogManager.getLogManager().getLogger("") }

fun setRootLogLevel(level: Level) {
    // See the table at https://docs.oracle.com/javase/9/docs/api/java/lang/System.Logger.Level.html.
    rootLogger.level = when (level) {
        Level.ALL -> java.util.logging.Level.ALL
        Level.TRACE -> java.util.logging.Level.FINER
        Level.DEBUG -> java.util.logging.Level.FINE
        Level.INFO -> java.util.logging.Level.INFO
        Level.WARNING -> java.util.logging.Level.WARNING
        Level.ERROR -> java.util.logging.Level.SEVERE
        Level.OFF -> java.util.logging.Level.OFF
    }
}
