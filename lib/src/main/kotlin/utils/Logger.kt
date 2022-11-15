package dev.schuberth.stan.utils

import java.lang.System.Logger.Level
import java.util.concurrent.ConcurrentHashMap

private val loggerCache = ConcurrentHashMap<String, System.Logger>().also {
    // See: https://docs.oracle.com/javase/7/docs/api/java/util/logging/SimpleFormatter.html#format(java.util.logging.LogRecord)
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
