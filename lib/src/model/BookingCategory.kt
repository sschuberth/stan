package dev.schuberth.stan.model

import kotlinx.serialization.Serializable

/**
 * The category of a booking item. An item must only belong to a single category, if any.
 */
@Serializable
data class BookingCategory(
    val name: String,
    val regexes: List<String>,
    val minAmount: Float = Float.NEGATIVE_INFINITY,
    val maxAmount: Float = Float.POSITIVE_INFINITY
)
