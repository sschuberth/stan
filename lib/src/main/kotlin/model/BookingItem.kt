package dev.schuberth.stan.model

import java.time.LocalDate

import io.ks3.java.typealiases.LocalDateAsString

import kotlinx.serialization.Serializable

@Serializable
data class BookingItem(
    val postDate: LocalDateAsString,
    val valueDate: LocalDateAsString,
    val info: MutableList<String>,
    val amount: Float,
    val type: BookingType,
    val category: String? = null
) {
    companion object {
        @JvmField
        val EMPTY = BookingItem(
            postDate = LocalDate.EPOCH,
            valueDate = LocalDate.EPOCH,
            info = mutableListOf(),
            amount = Float.NaN,
            type = BookingType.OTHER,
            category = null
        )
    }
}

fun List<String>.joinInfo(separator: String = ", "): String {
    val dashRegex = Regex("([A-Z-a-z]{2,})-$separator([A-Z][A-Za-z])")
    val hyphenationRegex = Regex("([a-z]{2,})-$separator([a-z]{2,})")

    return joinToString(separator) { it.trim() }
        .replace(dashRegex, "$1-$2")
        .replace(hyphenationRegex, "$1$2")
}
