package dev.schuberth.stan

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file

import dev.schuberth.stan.exporters.Exporter
import dev.schuberth.stan.exporters.JsonExporter
import dev.schuberth.stan.exporters.OfxV1Exporter
import dev.schuberth.stan.model.Statement
import dev.schuberth.stan.parsers.PostbankPDFParser

import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.text.ParseException

import kotlin.system.exitProcess

fun File.getExisting(): File? {
    var current = absoluteFile
    while (current != null && !current.exists()) {
        current = current.parentFile
    }
    return current
}

class Stan : CliktCommand() {
    enum class ExportFormat(val exporter: Exporter) {
        JSON(JsonExporter()),
        OFX(OfxV1Exporter());

        override fun toString() = name.toLowerCase()
    }

    private val statementFiles by argument()
        .file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = true)
        .multiple()

    private val exportFormat by option(
        "--export-format", "-f",
        help = "The data format used for dependency information."
    ).enum<ExportFormat>()

    override fun run() {
        if (statementFiles.isEmpty()) throw UsageError("No statement file(s) specified.", statusCode = 1)

        val statements = mutableListOf<Statement>()

        statementFiles.forEach { glob ->
            val globPath = glob.absoluteFile.invariantSeparatorsPath
            val matcher = FileSystems.getDefault().getPathMatcher("glob:$globPath")

            glob.getExisting()?.walkBottomUp()?.filter {
                matcher.matches(it.toPath())
            }?.forEach {
                try {
                    val st = PostbankPDFParser.parse(it)
                    println("Successfully parsed statement '$it' dated from ${st.fromDate} to ${st.toDate}.")
                    statements.add(st)
                } catch (e: ParseException) {
                    System.err.println("Error parsing '$it'.")
                    e.printStackTrace()
                }
            }
        }

        println("Parsed ${statements.size} statement(s) in total.")

        statements.sort()

        var statementIterator = statements.iterator()
        if (!statementIterator.hasNext()) {
            System.err.println("No statements found.")
            exitProcess(1)
        }

        var curr = statementIterator.next()
        var next: Statement
        while (statementIterator.hasNext()) {
            next = statementIterator.next()

            if (curr.toDate.plusDays(1) != next.fromDate) {
                System.err.println("Statements '${curr.filename}' and '${next.filename}' are not consecutive.")
                exitProcess(1)
            }

            if (curr.balanceNew != next.balanceOld) {
                System.err.println("Balances of statements '${curr.filename}' and '${next.filename}' are not consistent.")
                exitProcess(1)
            }

            curr = next
        }

        println("Consistency checks passed successfully.")

        exportFormat?.let {
            statementIterator = statements.iterator()
            while (statementIterator.hasNext()) {
                val statement = statementIterator.next()
                val exportName = statement.filename.substringBeforeLast(".") + "." + exportFormat.toString()

                println("Exporting\n    ${statement.filename}\nto\n    $exportName")
                it.exporter.write(statement, FileOutputStream(exportName))
                println("done.")
            }
        }
    }
}

fun main(args: Array<String>) = Stan().main(args)
