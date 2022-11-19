package dev.schuberth.stan.utils

class PatternMatching(private val value: String) {
    operator fun Regex.invoke(block: MatchResult.() -> Unit) = matchEntire(value)?.block()

    operator fun String.invoke(block: String.() -> Unit) = value.withoutPrefix(this)?.block()
}

fun whenMatch(value: String, block: PatternMatching.() -> Unit) = PatternMatching(value).block()

fun String.withoutPrefix(prefix: String) = removePrefix(prefix).takeIf { it != this }

fun String.withoutSuffix(suffix: String) = removeSuffix(suffix).takeIf { it != this }
