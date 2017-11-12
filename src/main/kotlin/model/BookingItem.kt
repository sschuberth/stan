package com.github.sschuberth.stan.model

import java.time.LocalDate

class BookingItem(
        val postDate: LocalDate,
        val valueDate: LocalDate,
        infoLine: String,
        val amount: Float
) {
    val info = mutableListOf(infoLine)

    override fun toString(): String {
        val s = StringBuilder("""
            {
            	"postDate": "$postDate",
            	"valueDate": "$valueDate",
            	"amount": "$amount",
            	"info": [

            """.trimIndent()
        )

        val infoIterator = info.iterator()
        while (infoIterator.hasNext()) {
            s.append("\t\t")

            // Quote the info line.
            s.append("\"")
            s.append(infoIterator.next())
            s.append("\"")

            if (infoIterator.hasNext()) {
                s.append(",")
            }

            s.append("\n")
        }

        s.append("\t]\n")
        s.append("}")

        return s.toString()
    }
}
