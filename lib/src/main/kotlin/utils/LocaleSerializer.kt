package dev.schuberth.stan.utils

import io.ks3.standard.stringSerializer

import java.util.Locale

import kotlinx.serialization.KSerializer

/**
 * A (de-)serializer for Java [Locale] instances from / to strings.
 */
object LocaleSerializer : KSerializer<Locale> by stringSerializer(::Locale)
