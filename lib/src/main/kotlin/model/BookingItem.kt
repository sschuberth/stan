@file:UseSerializers(dev.schuberth.stan.utils.LocalDateSerializer::class)

package dev.schuberth.stan.model

import java.time.LocalDate

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class BookingItem(
    val postDate: LocalDate,
    val valueDate: LocalDate,
    val info: MutableList<String>,
    val amount: Float,
    val type: BookingType,
    val category: String? = null
) {
    companion object {
        val EMPTY = BookingItem(
            postDate = LocalDate.EPOCH,
            valueDate = LocalDate.EPOCH,
            info = mutableListOf(),
            amount = Float.NaN,
            type = BookingType.UNKNOWN,
            category = null
        )
    }

    fun joinInfo(separator: String = ", "): String {
        val dashRegex = Regex("([A-Z-a-z]{2,})-$separator([A-Z][A-Za-z])")
        val hyphenationRegex = Regex("([a-z]{2,})-$separator([a-z]{2,})")

        return info.joinToString(separator) { it.trim() }
            .replace(dashRegex, "$1-$2")
            .replace(hyphenationRegex, "$1$2")
    }
}
