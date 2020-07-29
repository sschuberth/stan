package dev.schuberth.stan.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file

import dev.schuberth.stan.exporters.*
import dev.schuberth.stan.model.Configuration
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.parsers.PostbankPDFParser

import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.text.ParseException

import kotlin.system.exitProcess

fun File.getExisting(): File? {
    var current: File? = absoluteFile
    while (current != null && !current.exists()) {
        current = current.parentFile
    }
    return current
}

class Stan : CliktCommand() {
    enum class ExportFormat(val exporter: Exporter) {
        CSV(CsvExporter()),
        JSON(JsonExporter()),
        MT940(Mt940Exporter()),
        OFX(OfxV1Exporter()),
        QIF(QifExporter())
    }

    private val userHome by lazy {
        val userHome = System.getProperty("user.home")

        val fixedUserHome = if (userHome.isNullOrBlank() || userHome == "?") {
            listOfNotNull(
                System.getenv("HOME"),
                System.getenv("USERPROFILE")
            ).first { it.isNotBlank() }
        } else {
            userHome
        }

        File(fixedUserHome)
    }

    private val configFile by option(
        "--config-file", "-c",
        help = "The configuration file to use."
    ).file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = true)
        .default(userHome.resolve(".config/stan/config.json"))

    private val exportFormat by option(
        "--export-format", "-f",
        help = "The data format used for dependency information. If none is specified, only consistency checks on " +
                "statements will be performed."
    ).enum<ExportFormat>()

    private val outputDir by option(
        "--output-dir", "-o",
        help = "The directory to which output files will be written to."
    ).file(mustExist = false, canBeFile = false, canBeDir = true, mustBeReadable = false, mustBeWritable = true)
        .default(File("."))

    private val statementGlobs by argument()
        .file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = true)
        .multiple()

    override fun run() {
        if (statementGlobs.isEmpty()) throw UsageError("No statement file(s) specified.", statusCode = 1)

        println("Parsing statements...")

        val config = if (configFile.isFile) {
            Configuration.load(configFile)
        } else {
            Configuration.loadDefault()
        }

        val parser = PostbankPDFParser(config)
        val statements = sortedMapOf<Statement, File>()

        statementGlobs.forEach { glob ->
            val globPath = glob.absoluteFile.invariantSeparatorsPath
            val matcher = FileSystems.getDefault().getPathMatcher("glob:$globPath")

            glob.getExisting()?.walkBottomUp()?.filter {
                matcher.matches(it.toPath())
            }?.forEach {
                val file = it.normalize()

                try {
                    val st = parser.parse(file)
                    println("Successfully parsed statement\n\t$file\ndated from ${st.fromDate} to ${st.toDate}.")
                    statements[st] = file
                } catch (e: ParseException) {
                    System.err.println("Error parsing '$file'.")
                    e.printStackTrace()
                }
            }
        }

        println("Parsed ${statements.size} statement(s) in total.\n")

        if (statements.isEmpty()) {
            System.err.println("No statements found.")
            exitProcess(1)
        }

        println("Checking statements for consistency...")

        statements.keys.zipWithNext().forEach { (curr, next) ->
            if (curr.toDate.plusDays(1) != next.fromDate) {
                System.err.println("Statements '${curr.filename}' and '${next.filename}' are not consecutive.")
                exitProcess(1)
            }

            if (curr.balanceNew != next.balanceOld) {
                System.err.println(
                    "Balances of statements '${curr.filename}' and '${next.filename}' are not consistent."
                )
                exitProcess(1)
            }
        }

        println("All statements passed the consistency checks.\n")

        exportFormat?.let { format ->
            println("Exporting ${format.name} files...")

            statements.forEach { (statement, file) ->
                val exportName = "${statement.filename.substringBeforeLast(".")}.${format.exporter.extension}"
                val exportFile = outputDir.absoluteFile.normalize().resolve(exportName)

                format.exporter.write(statement, FileOutputStream(exportFile))
                println("Successfully exported\n\t$file\nto\n\t$exportFile")
            }

            println("Exported ${statements.size} statement(s) in total.\n")
        }
    }
}

fun main(args: Array<String>) = Stan().main(args)
