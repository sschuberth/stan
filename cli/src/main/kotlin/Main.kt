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

import dev.schuberth.stan.model.Configuration
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.parsers.Parser

import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.FileSystems
import java.text.ParseException

import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

fun File.getExisting(): File? {
    var current: File? = absoluteFile
    while (current != null && !current.exists()) {
        current = current.parentFile
    }
    return current
}

class Stan : CliktCommand(invokeWithoutSubcommand = true) {
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
                "arbitrary key-value pair. For example: -P PostbankPDF=textOutputDir=text/output/dir"
    ).splitPair().convert { (format, option) ->
        require(format in Parser.ALL) {
            "Parser format '$format' must be one of ${Parser.ALL.keys}."
        }

        format to Pair(option.substringBefore("="), option.substringAfter("=", ""))
    }.multiple()

    private val statementGlobs by argument().multiple()

    init {
        context {
            helpFormatter = CliktHelpFormatter(requiredOptionMarker = "*", showDefaultValues = true)
        }

        subcommands(ExportCommand())
    }

    override fun run() {
        if (statementGlobs.isEmpty()) throw UsageError("No statement file(s) specified.")

        val configModule = module {
            single {
                if (configFile.isFile) {
                    Configuration.load(configFile)
                } else {
                    Configuration.loadDefault()
                }
            }
        }

        startKoin {
            modules(configModule)
        }

        println("Parsing statements...")

        // Merge the list of pairs into a map which contains each format only once associated to all its options.
        val parserOptionsMap = sortedMapOf<String, MutableMap<String, String>>(String.CASE_INSENSITIVE_ORDER)

        parserOptions.forEach { (format, option) ->
            val parserSpecificOptionsMap = parserOptionsMap.getOrPut(format) { mutableMapOf() }
            parserSpecificOptionsMap[option.first] = option.second
        }

        // TODO: Do not hard-code this once multiple parsers are supported.
        val parser = Parser.ALL["PostbankPDF"]
            ?: throw IllegalArgumentException("Cannot instantiate PostbankPdf parser.")

        val allStatements = mutableListOf<Statement>()

        statementGlobs.forEach { globPattern ->
            val glob = File(globPattern)
            val globPath = glob.absoluteFile.invariantSeparatorsPath
            val matcher = FileSystems.getDefault().getPathMatcher("glob:$globPath")

            glob.getExisting()?.walkBottomUp()?.filter {
                matcher.matches(it.toPath())
            }?.forEach {
                val file = it.normalize()

                try {
                    // TODO: Do not hard-code this once multiple parsers are supported.
                    val statementsFromFile = parser.parse(file, parserOptionsMap["PostbankPDF"].orEmpty())

                    println(
                        "Successfully parsed statement\n\t$file\ndated from ${statementsFromFile.fromDate} to " +
                            "${statementsFromFile.toDate}."
                    )

                    allStatements += statementsFromFile
                } catch (e: ParseException) {
                    System.err.println("Error parsing '$file'.")
                    e.printStackTrace()
                }
            }
        }

        println("Parsed ${allStatements.size} statement(s) in total.\n")

        if (allStatements.isEmpty()) {
            System.err.println("No statements found.")
            throw ProgramResult(2)
        }

        println("Checking statements for consistency...")

        allStatements.sort()

        allStatements.zipWithNext().forEach { (curr, next) ->
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

        currentContext.findOrSetObject { allStatements }
    }
}

fun main(args: Array<String>) = Stan().main(args)
