package dev.schuberth.stan.utils

import java.time.LocalDate

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("WithCustomDefault", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder) = LocalDate.parse(decoder.decodeString())
}
