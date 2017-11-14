package com.github.sschuberth.stan

import com.github.sschuberth.stan.model.Statement
import com.github.sschuberth.stan.parsers.PostbankPDFParser

import java.io.File
import java.io.IOException
import java.nio.file.Files.newDirectoryStream
import java.text.ParseException
import java.util.Collections

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val statements = mutableListOf<Statement>()

        args.forEach { arg ->
            val file = File(arg)

            try {
                newDirectoryStream(file.absoluteFile.parentFile.toPath(), file.name).use { stream ->
                    stream.forEach { filename ->
                        try {
                            val st = PostbankPDFParser.parse(filename.toString())
                            println("Successfully parsed statement '" + filename + "' dated from " + st.fromDate + " to " + st.toDate + ".")
                            statements.add(st)
                        } catch (e: ParseException) {
                            System.err.println("Error parsing '$filename'.")
                            e.printStackTrace()
                        }
                    }
                    println("Parsed " + statements.size + " statement(s) in total.")
                }
            } catch (e: IOException) {
                System.err.println("Error opening '$arg'.")
            }
        }

        Collections.sort(statements)

        var statementIterator = statements.iterator()
        if (!statementIterator.hasNext()) {
            System.err.println("No statements found.")
            System.exit(1)
        }

        var curr = statementIterator.next()
        var next: Statement
        while (statementIterator.hasNext()) {
            next = statementIterator.next()

            if (curr.toDate.plusDays(1) != next.fromDate) {
                System.err.println("Statements '" + curr.filename + "' and '" + next.filename + "' are not consecutive.")
                System.exit(1)
            }

            if (curr.balanceNew != next.balanceOld) {
                System.err.println("Balances of statements '" + curr.filename + "' and '" + next.filename + "' are not consistent.")
                System.exit(1)
            }

            curr = next
        }

        statementIterator = statements.iterator()
        while (statementIterator.hasNext()) {
            println(statementIterator.next().toString())
        }
    }
}
