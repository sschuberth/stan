package dev.schuberth.stan.model

import dev.schuberth.stan.utils.JSON

import java.io.File
import java.io.InputStream

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromStream

@Suppress("unused")
@Serializable
data class Configuration(val regexOptions: Set<RegexOption>, val bookingCategories: List<BookingCategory>) {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        val EMPTY = Configuration(regexOptions = emptySet(), bookingCategories = emptyList())

        fun load(configStream: InputStream) = configStream.use { JSON.decodeFromStream<Configuration>(it) }

        fun load(configFile: File) = load(configFile.inputStream())

        fun load(configResource: String) = load(Configuration::class.java.getResourceAsStream(configResource))

        fun loadDefault() = load("/config.json")
    }

    fun findBookingCategory(item: BookingItem) =
        bookingCategories.find {
            val regex = Regex(it.regexes.joinToString("|", ".*(", ").*"), regexOptions)
            regex.matches(item.joinedInfo) && it.minAmount <= item.amount && item.amount < it.maxAmount
        }

    fun save(configFile: File) = configFile.writeText(JSON.encodeToString(this))
}
