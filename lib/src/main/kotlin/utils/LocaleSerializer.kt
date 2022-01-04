package dev.schuberth.stan.utils

import java.util.Locale

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializer(Locale::class)
object LocaleSerializer : KSerializer<Locale> {
    override fun serialize(encoder: Encoder, value: Locale) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Locale = Locale(decoder.decodeString())
}
