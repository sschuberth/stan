@file:UseSerializers(dev.schuberth.stan.utils.LocalDateSerializer::class)

package dev.schuberth.stan.model

import java.time.LocalDate

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

private const val JOIN_SEPARATOR = ", "
private val INFO_DASH_PATTERN = Regex("([A-Z-a-z]{2,})-$JOIN_SEPARATOR([A-Z][A-Za-z])")
private val INFO_HYPHENATION_PATTERN = Regex("([a-z]{2,})-$JOIN_SEPARATOR([a-z]{2,})")

@Serializable
data class BookingItem(
    val postDate: LocalDate,
    val valueDate: LocalDate,
    val info: MutableList<String>,
    val amount: Float,
    val type: BookingType,
    val category: String? = null
) {
    val joinedInfo by lazy {
        info.joinToString(JOIN_SEPARATOR) { it.trim() }
            .replace(INFO_DASH_PATTERN, "$1-$2")
            .replace(INFO_HYPHENATION_PATTERN, "$1$2")
    }
}
