package dev.schuberth.stan.utils

import java.lang.System.Logger.Level
import java.util.concurrent.ConcurrentHashMap

private val loggerCache = ConcurrentHashMap<String, System.Logger>().also {
    // The System.Logger API by default uses the java.util.logging.Logger implementation, so formatting needs to be
    // customized via java.util.logging.* properties, see
    // https://docs.oracle.com/cd/E57471_01/bigData.100/data_processing_bdd/src/rdp_logging_config.html
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1\$tF %1\$tT.%1\$tL %4$-7s %3\$s - %5\$s%n")
}

interface Logger {
    val logger: System.Logger
        get() = loggerCache.getOrPut(javaClass.name) { System.getLogger(javaClass.name) }

    fun System.Logger.trace(message: () -> String) = log(Level.TRACE, message)
    fun System.Logger.debug(message: () -> String) = log(Level.DEBUG, message)
    fun System.Logger.info(message: () -> String) = log(Level.INFO, message)
    fun System.Logger.warn(message: () -> String) = log(Level.WARNING, message)
    fun System.Logger.error(message: () -> String) = log(Level.ERROR, message)
}
