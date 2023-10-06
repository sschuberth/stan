package dev.schuberth.stan.utils

import java.lang.System.Logger.Level
import java.util.concurrent.ConcurrentHashMap

private val loggerCache = ConcurrentHashMap<String, System.Logger>()

interface Logger {
    val logger: System.Logger
        get() = loggerCache.getOrPut(javaClass.name) { System.getLogger(javaClass.name) }

    fun System.Logger.trace(message: () -> String) = log(Level.TRACE, message)
    fun System.Logger.debug(message: () -> String) = log(Level.DEBUG, message)
    fun System.Logger.info(message: () -> String) = log(Level.INFO, message)
    fun System.Logger.warn(message: () -> String) = log(Level.WARNING, message)
    fun System.Logger.error(message: () -> String) = log(Level.ERROR, message)
}
