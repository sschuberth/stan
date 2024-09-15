@file:Suppress("ArgumentListWrapping")

package dev.schuberth.stan.cli

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.splitPair
import com.github.ajalt.clikt.parameters.types.file

import dev.schuberth.stan.Exporter
import dev.schuberth.stan.model.Statement

import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

class ExportCommand : CliktCommand("export") {
    override fun help(context: Context) = "Export statements to different formats."

    private val exportFormats by option(
        "--format", "-f",
        help = "The data format to export to, must be one of ${Exporter.ALL.keys}. If none is specified only " +
                "consistency checks on statements are performed."
    ).convert { format ->
        Exporter.ALL[format]
            ?: throw BadParameterValue("Export format '$format' must be one of ${Exporter.ALL.keys}.")
    }.multiple()

    private val exportOptions by option(
        "--option", "-E",
        help = "An export format specific option. The key is the (case-insensitive) name of the export format, and " +
                "the value is an arbitrary key-value pair. For example: -E CSV=separator=;"
    ).splitPair().convert { (format, option) ->
        require(format in Exporter.ALL) {
            "Export format '$format' must be one of ${Exporter.ALL.keys}."
        }

        format to Pair(option.substringBefore("="), option.substringAfter("=", ""))
    }.multiple()

    private val outputDir by option(
        "--output-dir", "-o",
        help = "The directory to which output files will be written to."
    ).file(mustExist = false, canBeFile = false, canBeDir = true, mustBeReadable = false, mustBeWritable = true)

    private val statements by requireObject<Set<Statement>>()

    override fun run() {
        // Merge the list of pairs into a map which contains each format only once associated to all its options.
        val exportOptionsMap = sortedMapOf<String, MutableMap<String, String>>(String.CASE_INSENSITIVE_ORDER)

        exportOptions.forEach { (format, option) ->
            val exportSpecificOptionsMap = exportOptionsMap.getOrPut(format) { mutableMapOf() }
            exportSpecificOptionsMap[option.first] = option.second
        }

        // Export to all specified formats.
        exportFormats.forEach { exporter ->
            val options = exportOptionsMap[exporter.name].orEmpty()

            println("Exporting ${exporter.name} files...")

            statements.forEach { statement ->
                val exportName = "${statement.filename.substringBeforeLast(".")}.${exporter.extension}"
                val exportFile = outputDir?.absoluteFile?.normalize()?.resolve(exportName)
                val outputStream = exportFile?.let { FileOutputStream(it) } ?: ByteArrayOutputStream()

                exporter.write(statement, outputStream, options)

                when (outputStream) {
                    is FileOutputStream -> println("Successfully exported\n\t$exportFile")
                    is ByteArrayOutputStream -> println(outputStream.toString())
                }
            }

            println("Exported ${statements.size} statement(s) in total.\n")
        }
    }
}
