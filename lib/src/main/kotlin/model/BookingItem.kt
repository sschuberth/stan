@file:UseSerializers(LocalDateSerializer::class)

package dev.schuberth.stan.model

import java.time.LocalDate

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
class BookingItem(
    val postDate: LocalDate,
    val valueDate: LocalDate,
    val info: MutableList<String>,
    val amount: Float
)
