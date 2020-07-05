@file:UseSerializers(dev.schuberth.stan.utils.RegexSerializer::class)

package dev.schuberth.stan.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class BookingCategoryMatcher(
    val regex: Regex,
    val category: String
)
