package dev.schuberth.stan.utils

import java.util.Locale

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializer(forClass = Locale::class)
object LocaleSerializer : KSerializer<Locale> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("WithCustomDefault", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Locale) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder) = Locale(decoder.decodeString())
}
