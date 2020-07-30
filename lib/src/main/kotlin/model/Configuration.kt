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
        val EMPTY = Configuration(emptyList())

        fun load(configStream: InputStream) =
            JSON.parse(serializer(), configStream.bufferedReader().use(BufferedReader::readText))

        fun load(configFile: File) = load(configFile.inputStream())

        fun load(configResource: String) = load(Configuration::class.java.getResourceAsStream(configResource))

        fun loadDefault() = load("/config.json")
    }

    fun findBookingCategory(item: BookingItem) =
        bookingCategories.find {
            val regex = Regex(it.regexes.joinToString("|", ".*(", ").*"), RegexOption.IGNORE_CASE)
            regex.matches(item.joinedInfo) && it.minAmount <= item.amount && item.amount < it.maxAmount
        }

    fun save(configFile: File) = configFile.writeText(JSON.stringify(serializer(), this))
}
