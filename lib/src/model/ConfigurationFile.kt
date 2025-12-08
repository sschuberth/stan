package dev.schuberth.stan.model

import dev.schuberth.stan.utils.JSON

import io.ks3.java.typealiases.FileAsString

import java.io.File
import java.io.InputStream
import java.nio.file.FileSystems

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream

@Serializable
data class ConfigurationFile(
    val statementGlobs: Set<FileAsString> = emptySet(),
    val regexOptions: Set<RegexOption> = emptySet(),
    val bookingCategories: List<BookingCategory> = emptyList()
) : Configuration {
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        fun load(configStream: InputStream): Configuration =
            configStream.use { JSON.decodeFromStream<ConfigurationFile>(it) }

        fun load(configFile: File): Configuration = load(configFile.inputStream())

        fun load(configResource: String): Configuration =
            load(Configuration::class.java.getResourceAsStream(configResource))

        fun loadDefault(): Configuration = load("/config.json")

        fun resolveGlobs(files: Set<File>): Set<File> = files.flatMapTo(mutableSetOf()) { globFile ->
            if (globFile.isFile) {
                sequenceOf(globFile)
            } else {
                val globPath = globFile.absoluteFile.invariantSeparatorsPath
                val matcher = FileSystems.getDefault().getPathMatcher("glob:$globPath")

                var walkRoot: File? = globFile
                while (walkRoot?.isDirectory == false) walkRoot = walkRoot.parentFile

                walkRoot?.walkBottomUp()?.filter {
                    matcher.matches(it.toPath())
                }.orEmpty().sorted()
            }
        }
    }

    override fun getStatementFiles(): Set<File> = resolveGlobs(statementGlobs)

    override fun findBookingCategory(item: BookingItem): BookingCategory? = bookingCategories.find {
        val regex = Regex(it.regexes.joinToString("|", ".*(", ").*"), regexOptions)
        regex.matches(item.info.joinInfo()) && it.minAmount <= item.amount && item.amount < it.maxAmount
    }
}
