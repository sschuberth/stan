package dev.schuberth.stan.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * A (de-)serializer for Java [File] instances from / to strings.
 */
object FileSerializer : KSerializer<File> by toStringSerializer(::File)

/**
 * A (de-)serializer for Java [LocalDate] instances from / to strings.
 */
object LocalDateSerializer : KSerializer<LocalDate> by toStringSerializer(LocalDate::parse)

/**
 * A (de-)serializer for Java [Locale] instances from / to strings.
 */
object LocaleSerializer : KSerializer<Locale> by toStringSerializer(::Locale)

/**
 * A convenience function for creating a [ToStringSerializer] whose name is derived from the class name.
 */
inline fun <reified T : Any> toStringSerializer(noinline create: (String) -> T): ToStringSerializer<T> =
    ToStringSerializer(T::class.java.name, create)

/**
 * A serializer with the given serial name that uses [Any] instance's [toString] function for serialization and the
 * given [create] function for deserialization.
 */
class ToStringSerializer<T : Any>(serialName: String, private val create: (String) -> T) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder) = create(decoder.decodeString())
}
