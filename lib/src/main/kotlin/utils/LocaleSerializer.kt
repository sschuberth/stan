package dev.schuberth.stan.utils

import java.util.Locale

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PrimitiveDescriptor
import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializer

@Serializer(forClass = Locale::class)
object LocaleSerializer : KSerializer<Locale> {
    override val descriptor: SerialDescriptor =
        PrimitiveDescriptor("WithCustomDefault", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Locale) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder) = Locale(decoder.decodeString())
}
