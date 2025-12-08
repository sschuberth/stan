package dev.schuberth.stan.utils

/**
 * Call [also] only if the receiver is null, e.g. for error handling, and return the receiver in any case.
 */
inline fun <T> T.alsoIfNull(block: (T) -> Unit): T = this ?: also(block)
