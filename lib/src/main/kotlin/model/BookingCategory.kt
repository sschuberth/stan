@file:UseSerializers(dev.schuberth.stan.utils.RegexSerializer::class)

package dev.schuberth.stan.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * The category of a booking item. An item must only belong to a single category, if any.
 */
@Serializable
data class BookingCategory(
    val name: String,
    val regex: Regex,
    val minAmount: Float = Float.NEGATIVE_INFINITY,
    val maxAmount: Float = Float.POSITIVE_INFINITY
)
