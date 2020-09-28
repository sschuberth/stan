package dev.schuberth.stan.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.splitPair
import com.github.ajalt.clikt.parameters.types.file

import dev.schuberth.stan.exporters.*
import dev.schuberth.stan.model.Configuration
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.parsers.*

import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalArgumentException
import java.nio.file.FileSystems
import java.text.ParseException

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

fun File.getExisting(): File? {
    var current: File? = absoluteFile
    while (current != null && !current.exists()) {
        current = current.parentFile
    }
    return current
}

class Stan : CliktCommand() {
    @Suppress("Unused")
    sealed class ParserFactory<T : Parser>(private val parser: KClass<T>) {
        companion object {
            val ALL = ParserFactory::class.sealedSubclasses.associateBy { it.simpleName!!.toUpperCase() }
        }

        object PostbankPdf : ParserFactory<PostbankPdfParser>(PostbankPdfParser::class)

        fun create(config: Configuration) = parser.primaryConstructor?.call(config)
    }

    @Suppress("Unused")
    sealed class ExporterFactory<T : Exporter>(private val exporter: KClass<T>) {
        companion object {
            val ALL = ExporterFactory::class.sealedSubclasses.associateBy { it.simpleName!!.toUpperCase() }
        }

        object Csv : ExporterFactory<CsvExporter>(CsvExporter::class)
        object Excel : ExporterFactory<ExcelExporter>(ExcelExporter::class)
        object Json : ExporterFactory<JsonExporter>(JsonExporter::class)
        object Mt940 : ExporterFactory<Mt940Exporter>(Mt940Exporter::class)
        object Ofx : ExporterFactory<OfxV1Exporter>(OfxV1Exporter::class)
        object Qif : ExporterFactory<QifExporter>(QifExporter::class)

        fun create() = exporter.createInstance()
    }

    private val userHome by lazy {
        val fixedUserHome = System.getProperty("user.home").takeUnless { it.isBlank() || it == "?" } ?: run {
            listOfNotNull(
                System.getenv("HOME"),
                System.getenv("USERPROFILE")
            ).first { it.isNotBlank() }
        }

        File(fixedUserHome)
    }

    private val configFile by option(
        "--config-file", "-c",
        help = "The configuration file to use."
    ).file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = true)
        .default(userHome.resolve(".config/stan/config.json"))

    private val parserOptions by option(
        "--parser-option", "-P",
        help = "A parser specific option. The key is the (case-insensitive) name of the parser, and the value is an " +
                "arbitrary key-value pair. For example: -P PostbankPDF=textOutput=true"
    ).splitPair().convert { (format, option) ->
        val upperCaseFormat = format.toUpperCase()

        require(upperCaseFormat in ParserFactory.ALL.keys) {
            "Parser format '$upperCaseFormat' must be one of ${ParserFactory.ALL.keys}."
        }

        upperCaseFormat to Pair(option.substringBefore("="), option.substringAfter("=", ""))
    }.multiple()

    private val exportFormats by option(
        "--export-format", "-f",
        help = "The data format to export to. If none is specified only consistency checks on statements are performed."
    ).convert { format ->
        val upperCaseFormat = format.toUpperCase()
        val factory = ExporterFactory.ALL[upperCaseFormat]?.objectInstance
        factory ?: throw UsageError("Export format '$upperCaseFormat' must be one of ${ExporterFactory.ALL.keys}.")
        upperCaseFormat to factory.create()
    }.multiple()

    private val exportOptions by option(
        "--export-option", "-E",
        help = "An export format specific option. The key is the (case-insensitive) name of the export format, and " +
                "the value is an arbitrary key-value pair. For example: -E CSV=separator=;"
    ).splitPair().convert { (format, option) ->
        val upperCaseFormat = format.toUpperCase()

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

    private val statementGlobs by argument()
        .file(mustExist = false, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = false)
        .multiple()

    override fun run() {
        if (statementGlobs.isEmpty()) throw UsageError("No statement file(s) specified.")

        println("Parsing statements...")

        val config = if (configFile.isFile) {
            Configuration.load(configFile)
        } else {
            Configuration.loadDefault()
        }

        // Merge the list of pairs into a map which contains each format only once associated to all its options.
        val parserOptionsMap = mutableMapOf<String, MutableMap<String, String>>()

        parserOptions.forEach { (format, option) ->
            val parserSpecificOptionsMap = parserOptionsMap.getOrPut(format) { mutableMapOf() }
            parserSpecificOptionsMap[option.first] = option.second
        }

        // TODO: Do not hard-code this once multiple parsers are supported.
        val parser = ParserFactory.PostbankPdf.create(config)
            ?: throw IllegalArgumentException("Cannot instantiate PostbankPdf parser.")

        val statements = mutableListOf<Statement>()

        statementGlobs.forEach { glob ->
            val globPath = glob.absoluteFile.invariantSeparatorsPath
            val matcher = FileSystems.getDefault().getPathMatcher("glob:$globPath")

            glob.getExisting()?.walkBottomUp()?.filter {
                matcher.matches(it.toPath())
            }?.forEach {
                val file = it.normalize()

                try {
                    // TODO: Do not hard-code this once multiple parsers are supported.
                    val st = parser.parse(file, parserOptionsMap["POSTBANKPDF"].orEmpty())
                    println("Successfully parsed statement\n\t$file\ndated from ${st.fromDate} to ${st.toDate}.")
                    statements += st
                } catch (e: ParseException) {
                    System.err.println("Error parsing '$file'.")
                    e.printStackTrace()
                }
            }
        }

        println("Parsed ${statements.size} statement(s) in total.\n")

        if (statements.isEmpty()) {
            System.err.println("No statements found.")
            throw ProgramResult(2)
        }

        println("Checking statements for consistency...")

        statements.sort()

        statements.zipWithNext().forEach { (curr, next) ->
            if (curr.toDate.plusDays(1) != next.fromDate) {
                System.err.println("Statements '${curr.filename}' and '${next.filename}' are not consecutive.")
                throw ProgramResult(2)
            }

            if (curr.balanceNew != next.balanceOld) {
                System.err.println(
                    "Balances of statements '${curr.filename}' and '${next.filename}' are not consistent."
                )
                throw ProgramResult(2)
            }
        }

        println("All statements passed the consistency checks.\n")

        if (exportFormats.isEmpty()) return

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

fun main(args: Array<String>) = Stan().main(args)
