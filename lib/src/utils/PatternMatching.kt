package dev.schuberth.stan.utils

class PatternMatching(private val value: String) {
    private var handled = false

    operator fun Regex.invoke(always: Boolean = false, block: MatchResult.() -> Unit) {
        if (handled && !always) return
        matchEntire(value)?.block()?.also { handled = true }
    }

    operator fun String.invoke(always: Boolean = false, block: String.() -> Unit) {
        if (handled && !always) return
        value.withoutPrefix(this)?.block()?.also { handled = true }
    }

    fun otherwise(block: () -> Unit) {
        if (handled) return
        block()
        handled = true
    }
}

fun whenMatch(value: String, block: PatternMatching.() -> Unit) = PatternMatching(value).block()

fun String.withoutPrefix(prefix: String) = removePrefix(prefix).takeIf { it != this }

fun String.withoutSuffix(suffix: String) = removeSuffix(suffix).takeIf { it != this }
