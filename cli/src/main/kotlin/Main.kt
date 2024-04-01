@file:Suppress("ArgumentListWrapping")

package dev.schuberth.stan.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.splitPair
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file

import dev.schuberth.stan.Exporter
import dev.schuberth.stan.model.ConfigurationFile
import dev.schuberth.stan.Parser
import dev.schuberth.stan.utils.Logger
import dev.schuberth.stan.utils.alsoIfNull

import java.io.File
import java.lang.System.Logger.Level

import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class Main : CliktCommand(invokeWithoutSubcommand = true), Logger {
    private val userHome by lazy {
        val fixedUserHome = System.getProperty("user.home").takeUnless { it.isBlank() || it == "?" } ?: listOfNotNull(
            System.getenv("HOME"),
            System.getenv("USERPROFILE")
        ).first {
            it.isNotBlank()
        }

        File(fixedUserHome)
    }

    private val logLevel by option(
        "--log-level", "-l",
        help = "The log level to use."
    ).enum<Level>().default(Level.INFO)

    private val configFile by option(
        "--config-file", "-c",
        help = "The configuration file to use."
    ).file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = true)
        .default(userHome.resolve(".config/stan/config.json"))

    private val parserOptions by option(
        "--parser-option", "-p",
        help = "A parser specific option. The key is the (case-insensitive) name of the parser, and the value is an " +
                "arbitrary key-value pair. For example: -p PostbankPDF=textOutputDir=text/output/dir"
    ).splitPair().convert { (format, option) ->
        require(format in Parser.ALL) {
            "Parser format '$format' must be one of ${Parser.ALL.keys}."
        }

        format to Pair(option.substringBefore("="), option.substringAfter("=", ""))
    }.multiple()

    private val statementGlobs by argument().multiple()

    init {
        context {
            helpFormatter = { MordantHelpFormatter(context = it, requiredOptionMarker = "*", showDefaultValues = true) }
        }

        subcommands(ExportCommand(), FilterCommand())
    }

    override fun run() {
        setRootLogLevel(logLevel)

        logger.info { "Available parsers: ${Parser.ALL.keys.joinToString()}" }
        logger.info { "Available exporters: ${Exporter.ALL.keys.joinToString()}" }

        val config = if (configFile.isFile) {
            ConfigurationFile.load(configFile)
        } else {
            ConfigurationFile.loadDefault()
        }

        val statementFiles = config.getStatementFiles() + ConfigurationFile.resolveGlobs(
            statementGlobs.mapTo(mutableSetOf()) { File(it) }
        )

        if (statementFiles.isEmpty()) throw UsageError("No statement file(s) specified.")

        val configModule = module {
            single { config }
        }

        startKoin {
            modules(configModule)
        }

        // Merge the list of pairs into a map which contains each format only once associated to all its options.
        val parserOptionsMap = sortedMapOf<String, MutableMap<String, String>>(String.CASE_INSENSITIVE_ORDER)

        parserOptions.forEach { (format, option) ->
            val parserSpecificOptionsMap = parserOptionsMap.getOrPut(format) { mutableMapOf() }
            parserSpecificOptionsMap[option.first] = option.second
        }

        val parsedStatements = statementFiles.mapNotNull {
            val file = it.normalize()

            runCatching {
                val parserEntry = Parser.ALL.entries.find { (_, parser) -> parser.isApplicable(file) }
                parserEntry?.let { (name, parser) ->
                    print("Parsing statement '$file'... ")

                    parser.parse(file, parserOptionsMap[name].orEmpty()).also { statementsFromFile ->
                        println(
                            "recognized $name statement dated from ${statementsFromFile.fromDate} to " +
                                    "${statementsFromFile.toDate}."
                        )
                    }
                }.alsoIfNull {
                    println("No applicable parser found for file '$file'.")
                }
            }.onFailure { e ->
                System.err.println("Error parsing '$file'.")
                e.printStackTrace()
            }.getOrNull()
        }

        println("Successfully parsed ${parsedStatements.size} of ${statementFiles.size} statement(s).\n")

        if (parsedStatements.isEmpty()) {
            System.err.println("No statements found.")
            throw ProgramResult(2)
        }

        println("Checking parsed statements for consistency...")

        val sortedStatements = parsedStatements.toSortedSet(compareBy { it.fromDate })

        sortedStatements.zipWithNext().forEach { (curr, next) ->
            if (curr.bankId != next.bankId) {
                System.err.println(
                    "Statements '${curr.filename}' (${curr.bankId}) and '${next.filename}' (${next.bankId}) do not " +
                            "belong to the same bank."
                )
                throw ProgramResult(2)
            }

            if (curr.accountId != next.accountId) {
                System.err.println(
                    "Statements '${curr.filename}' (${curr.accountId}) and '${next.filename}' (${next.accountId}) do " +
                            "not belong to the same account."
                )
                throw ProgramResult(2)
            }

            if (curr.toDate.plusDays(1) != next.fromDate) {
                System.err.println(
                    "Statements '${curr.filename}' (${curr.toDate}) and '${next.filename}' (${next.fromDate}) are " +
                            "not consecutive."
                )
                throw ProgramResult(2)
            }

            if (curr.balanceNew != next.balanceOld) {
                System.err.println(
                    "Balances of statements '${curr.filename}' (${curr.balanceNew}) and '${next.filename}' " +
                            "(${next.balanceOld}) are not successive."
                )
                throw ProgramResult(2)
            }
        }

        println(
            "All ${sortedStatements.size} parsed statements of originally ${statementFiles.size} statements passed " +
                    "the consistency checks.\n"
        )

        currentContext.findOrSetObject { sortedStatements }
    }
}

fun main(args: Array<String>) = Main().main(args)
