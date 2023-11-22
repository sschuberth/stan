@file:Suppress("ConstructorParameterNaming", "Filename", "MatchingDeclarationName")

package dev.schuberth.stan.plugins.exporters.supa

import kotlinx.serialization.Serializable

@Serializable
data class Booking(
    /** Owner account currency. */
    val OwnrAcctCcy: String,

    /** Owner account IBAN. */
    val OwnrAcctIBAN: String,

    /** Owner account BIC. */
    val OwnrAcctBIC: String,

    /** Booking date (e.g. "2014-04-04"). */
    val BookgDt: String,

    /** Value date (e.g. "2014-04-04"). */
    val ValDt: String,

    /** Booking amount (e.g. "1.45"). */
    val Amt: String,

    /** Booking currency (e.g. "EUR"). */
    val AmtCcy: String,

    /** Credit / debit indicator. */
    val CdtDbtInd: CreditDebitIndicator,

    /** Reference information. */
    val RmtInf: String,

    /** Booking text. */
    val BookgTxt: String,

    /** Remitted account currency. */
    val RmtdAcctCtry: String,

    /** Remitted account IBAN. */
    val RmtdAcctIBAN: String,

    /** Remitted account BIC. */
    val RmtdAcctBIC: String
)

enum class CreditDebitIndicator {
    /** Credit. */
    CRDT,

    /** Debit. */
    DBIT;

    companion object {
        fun forAmount(amount: Float) = if (amount < 0) DBIT else CRDT
    }
}
