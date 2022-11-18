package dev.schuberth.stan.utils

class PatternMatching(private val value: String) {
    fun pattern(pattern: Regex, block: MatchResult.() -> Unit) = pattern.matchEntire(value)?.block()

    fun pattern(prefix: String, block: String.() -> Unit) = value.withoutPrefix(prefix)?.block()
}

fun whenMatch(value: String, block: PatternMatching.() -> Unit) = PatternMatching(value).block()

fun String.withoutPrefix(prefix: String) = removePrefix(prefix).takeIf { it != this }

fun String.withoutSuffix(suffix: String) = removeSuffix(suffix).takeIf { it != this }
