package dev.schuberth.stan.model

import dev.schuberth.stan.exporters.JSON

import java.io.BufferedReader
import java.io.File
import java.io.InputStream

import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
    val bookingCategories: List<BookingCategory>
) {
    companion object {
        private val INFO_HYPHENATION_PATTERN = Regex("([a-z]{2,})-([a-z]{2,})")

        val EMPTY = Configuration(emptyList())

        fun load(configStream: InputStream) =
            JSON.parse(serializer(), configStream.bufferedReader().use(BufferedReader::readText))

        fun load(configFile: File) = load(configFile.inputStream())

        fun load(configResource: String) = load(Configuration::class.java.getResourceAsStream(configResource))

        fun loadDefault() = load("/config.json")
    }

    fun findBookingCategory(item: BookingItem): BookingCategory? {
        val joinedInfo = item.info.joinToString("").replace(INFO_HYPHENATION_PATTERN, "\\1\\2")

        return bookingCategories.find {
            it.regex.matches(joinedInfo) && it.minAmount <= item.amount && item.amount < it.maxAmount
        }
    }

    fun save(configFile: File) = configFile.writeText(JSON.stringify(serializer(), this))
}
