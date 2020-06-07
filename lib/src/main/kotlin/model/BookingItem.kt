package dev.schuberth.stan.model

import java.time.LocalDate

class BookingItem(
    val postDate: LocalDate,
    val valueDate: LocalDate,
    infoLine: String,
    val amount: Float
) {
    val info = mutableListOf(infoLine)

    override fun toString() =
        buildString {
            append(
                """
                {
                  "postDate": "$postDate",
                  "valueDate": "$valueDate",
                  "amount": "$amount",
                  "info": [
    
                """.trimIndent()
            )

            val infoIterator = info.iterator()
            while (infoIterator.hasNext()) {
                append("    ")

                // Quote the info line.
                append("\"")
                append(infoIterator.next())
                append("\"")

                if (infoIterator.hasNext()) {
                    append(",")
                }

                append("\n")
            }

            append("  ]\n")
            append("}")
        }
}
