package dev.schuberth.stan.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
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
import java.lang.IllegalArgumentException
import java.nio.file.FileSystems
import java.text.ParseException

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

fun File.getExisting(): File? {
    var current: File? = absoluteFile
    while (current != null && !current.exists()) {
        current = current.parentFile
    }
    return current
}

class Stan : CliktCommand(invokeWithoutSubcommand = true) {
    @Suppress("Unused")
    sealed class ParserFactory<T : Parser>(private val parser: KClass<T>) {
        companion object {
            val ALL = ParserFactory::class.sealedSubclasses.associateBy { it.simpleName!!.uppercase() }
        }

        object PostbankPdf : ParserFactory<PostbankPdfParser>(PostbankPdfParser::class)

        fun create(config: Configuration) = parser.primaryConstructor?.call(config)
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
        val upperCaseFormat = format.uppercase()

        require(upperCaseFormat in ParserFactory.ALL.keys) {
            "Parser format '$upperCaseFormat' must be one of ${ParserFactory.ALL.keys}."
        }

        upperCaseFormat to Pair(option.substringBefore("="), option.substringAfter("=", ""))
    }.multiple()

    private val statementGlobs by argument()
        .file(mustExist = false, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = false)
        .multiple()

    init {
        context {
            helpFormatter = CliktHelpFormatter(requiredOptionMarker = "*", showDefaultValues = true)
        }

        subcommands(ExportCommand())
    }

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

        currentContext.findOrSetObject { statements }
    }
}

fun main(args: Array<String>) = Stan().main(args)
