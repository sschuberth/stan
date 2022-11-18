package dev.schuberth.stan.utils

class PatternMatching(private val value: String) {
    fun pattern(pattern: Regex, block: MatchResult.() -> Unit) = pattern.matchEntire(value)?.block()
}

fun whenMatch(value: String, block: PatternMatching.() -> Unit) = PatternMatching(value).block()
