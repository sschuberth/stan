package dev.schuberth.stan.model

import dev.schuberth.stan.exporters.JSON

import java.io.BufferedReader
import java.io.File
import java.io.InputStream

import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
    val bookingCategories: List<String>,
    val bookingCategoryMatchers: List<BookingCategoryMatcher>
) {
    companion object {
        val EMPTY = Configuration(emptyList(), emptyList())

        fun load(configStream: InputStream) =
            JSON.parse(serializer(), configStream.bufferedReader().use(BufferedReader::readText))

        fun load(configFile: File) = load(configFile.inputStream())

        fun load(configResource: String) = load(Configuration::class.java.getResourceAsStream(configResource))

        fun loadDefault() = load("/config.json")
    }

    fun isValid() = bookingCategoryMatchers.all { it.category in bookingCategories }

    fun save(configFile: File) = configFile.writeText(JSON.stringify(serializer(), this))
}
