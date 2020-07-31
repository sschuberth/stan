package dev.schuberth.stan.model

import dev.schuberth.stan.exporters.JSON

import java.io.BufferedReader
import java.io.File
import java.io.InputStream

import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
    val regexOptions: Set<RegexOption> = emptySet(),
    val bookingCategories: List<BookingCategory> = emptyList()
) {
    companion object {
        val EMPTY = Configuration()

        fun load(configStream: InputStream) =
            JSON.parse(serializer(), configStream.bufferedReader().use(BufferedReader::readText))

        fun load(configFile: File) = load(configFile.inputStream())

        fun load(configResource: String) = load(Configuration::class.java.getResourceAsStream(configResource))

        fun loadDefault() = load("/config.json")
    }

    fun findBookingCategory(item: BookingItem) =
        bookingCategories.find {
            val regex = Regex(it.regexes.joinToString("|", ".*(", ").*"), regexOptions)
            regex.matches(item.joinedInfo) && it.minAmount <= item.amount && item.amount < it.maxAmount
        }

    fun save(configFile: File) = configFile.writeText(JSON.stringify(serializer(), this))
}
