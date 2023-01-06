package dev.schuberth.stan.model

import dev.schuberth.stan.utils.JSON

import java.io.File
import java.io.InputStream

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream

@Serializable
data class ConfigurationFile(
    val regexOptions: Set<RegexOption> = emptySet(),
    val bookingCategories: List<BookingCategory> = emptyList()
) : Configuration {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        fun load(configStream: InputStream): Configuration =
            configStream.use { JSON.decodeFromStream<ConfigurationFile>(it) }

        fun load(configFile: File): Configuration =
            load(configFile.inputStream())

        fun load(configResource: String): Configuration =
            load(Configuration::class.java.getResourceAsStream(configResource))

        fun loadDefault(): Configuration =
            load("/config.json")
    }

    override fun findBookingCategory(item: BookingItem): BookingCategory? =
        bookingCategories.find {
            val regex = Regex(it.regexes.joinToString("|", ".*(", ").*"), regexOptions)
            regex.matches(item.joinedInfo) && it.minAmount <= item.amount && item.amount < it.maxAmount
        }
}
