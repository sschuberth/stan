package dev.schuberth.stan.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.splitPair
import com.github.ajalt.clikt.parameters.types.file

import dev.schuberth.stan.exporters.CsvExporter
import dev.schuberth.stan.exporters.ExcelExporter
import dev.schuberth.stan.exporters.Exporter
import dev.schuberth.stan.exporters.JsonExporter
import dev.schuberth.stan.exporters.Mt940Exporter
import dev.schuberth.stan.exporters.OfxV1Exporter
import dev.schuberth.stan.exporters.QifExporter
import dev.schuberth.stan.model.Statement

import java.io.File
import java.io.FileOutputStream

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class ExportCommand : CliktCommand(name = "export", help = "Export statements to different formats.") {
    @Suppress("Unused")
    sealed class ExporterFactory<T : Exporter>(private val exporter: KClass<T>) {
        companion object {
            val ALL = ExporterFactory::class.sealedSubclasses.associateBy { it.simpleName!!.uppercase() }
        }

        object Csv : ExporterFactory<CsvExporter>(CsvExporter::class)
        object Excel : ExporterFactory<ExcelExporter>(ExcelExporter::class)
        object Json : ExporterFactory<JsonExporter>(JsonExporter::class)
        object Mt940 : ExporterFactory<Mt940Exporter>(Mt940Exporter::class)
        object Ofx : ExporterFactory<OfxV1Exporter>(OfxV1Exporter::class)
        object Qif : ExporterFactory<QifExporter>(QifExporter::class)

        fun create() = exporter.createInstance()
    }

    private val exportFormats by option(
        "--format", "-f",
        help = "The data format to export to, must be one of ${ExporterFactory.ALL.keys}. If none is specified only " +
                "consistency checks on statements are performed."
    ).convert { format ->
        val upperCaseFormat = format.uppercase()
        val factory = ExporterFactory.ALL[upperCaseFormat]?.objectInstance
        factory ?: throw UsageError("Export format '$upperCaseFormat' must be one of ${ExporterFactory.ALL.keys}.")
        upperCaseFormat to factory.create()
    }.multiple()

    private val exportOptions by option(
        "--option", "-E",
        help = "An export format specific option. The key is the (case-insensitive) name of the export format, and " +
                "the value is an arbitrary key-value pair. For example: -E CSV=separator=;"
    ).splitPair().convert { (format, option) ->
        val upperCaseFormat = format.uppercase()

        require(upperCaseFormat in ExporterFactory.ALL.keys) {
            "Export format '$upperCaseFormat' must be one of ${ExporterFactory.ALL.keys}."
        }

        upperCaseFormat to Pair(option.substringBefore("="), option.substringAfter("=", ""))
    }.multiple()

    private val outputDir by option(
        "--output-dir", "-o",
        help = "The directory to which output files will be written to."
    ).file(mustExist = false, canBeFile = false, canBeDir = true, mustBeReadable = false, mustBeWritable = true)
        .default(File("."))

    private val statements by requireObject<List<Statement>>()

    override fun run() {
        // Merge the list of pairs into a map which contains each format only once associated to all its options.
        val exportOptionsMap = mutableMapOf<String, MutableMap<String, String>>()

        exportOptions.forEach { (format, option) ->
            val exportSpecificOptionsMap = exportOptionsMap.getOrPut(format) { mutableMapOf() }
            exportSpecificOptionsMap[option.first] = option.second
        }

        // Export to all specified formats.
        exportFormats.forEach { (format, exporter) ->
            val options = exportOptionsMap[format].orEmpty()

            println("Exporting $format files...")

            statements.forEach { statement ->
                val exportName = "${statement.filename.substringBeforeLast(".")}.${exporter.extension}"
                val exportFile = outputDir.absoluteFile.normalize().resolve(exportName)

                exporter.write(statement, FileOutputStream(exportFile), options)
                println("Successfully exported\n\t$exportFile")
            }

            println("Exported ${statements.size} statement(s) in total.\n")
        }
    }
}
