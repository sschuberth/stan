@file:Suppress("ArgumentListWrapping")

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

import dev.schuberth.stan.model.ConfigurationFile
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.parsers.Parser

import java.io.File
import java.text.ParseException

import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class Main : CliktCommand(invokeWithoutSubcommand = true) {
    private val userHome by lazy {
        val fixedUserHome = System.getProperty("user.home").takeUnless { it.isBlank() || it == "?" } ?: listOfNotNull(
            System.getenv("HOME"),
            System.getenv("USERPROFILE")
        ).first {
            it.isNotBlank()
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

    private val statementGlobs by argument().file().multiple()

    init {
        context {
            helpFormatter = CliktHelpFormatter(requiredOptionMarker = "*", showDefaultValues = true)
        }

        subcommands(ExportCommand())
    }

    override fun run() {
        val config = if (configFile.isFile) {
            ConfigurationFile.load(configFile)
        } else {
            ConfigurationFile.loadDefault()
        }

        val statementFiles = config.getStatementFiles() + ConfigurationFile.resolveGlobs(statementGlobs.toSet())
        if (statementFiles.isEmpty()) throw UsageError("No statement file(s) specified.")

        val configModule = module {
            single { config }
        }

        startKoin {
            modules(configModule)
        }

        println("Parsing statements with ${Parser.ALL.keys}...")

        // Merge the list of pairs into a map which contains each format only once associated to all its options.
        val parserOptionsMap = sortedMapOf<String, MutableMap<String, String>>(String.CASE_INSENSITIVE_ORDER)

        parserOptions.forEach { (format, option) ->
            val parserSpecificOptionsMap = parserOptionsMap.getOrPut(format) { mutableMapOf() }
            parserSpecificOptionsMap[option.first] = option.second
        }

        val parsedStatements = mutableListOf<Statement>()

        statementFiles.forEach nextFile@{
            val file = it.normalize()

            try {
                val parserEntry = Parser.ALL.entries.find { (_, parser) -> parser.isApplicable(file) }
                if (parserEntry == null) {
                    println("No applicable parser found for file '$file'.")
                    return@nextFile
                }

                val (name, parser) = parserEntry
                val statementsFromFile = parser.parse(file, parserOptionsMap[name].orEmpty())

                println(
                    "Successfully parsed $name statement '$file' dated from ${statementsFromFile.fromDate} to " +
                        "${statementsFromFile.toDate}."
                )

                parsedStatements += statementsFromFile
            } catch (e: ParseException) {
                System.err.println("Error parsing '$file'.")
                e.printStackTrace()
            }
        }

        println("Successfully parsed ${parsedStatements.size} of ${statementFiles.size} statement(s).\n")

        if (parsedStatements.isEmpty()) {
            System.err.println("No statements found.")
            throw ProgramResult(2)
        }

        println("Checking parsed statements for consistency...")

        parsedStatements.sort()

        parsedStatements.zipWithNext().forEach { (curr, next) ->
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

        println(
            "All ${parsedStatements.size} parsed statements of originally ${statementFiles.size} statements passed " +
                    "the consistency checks.\n"
        )

        currentContext.findOrSetObject { parsedStatements }
    }
}

fun main(args: Array<String>) = Main().main(args)
