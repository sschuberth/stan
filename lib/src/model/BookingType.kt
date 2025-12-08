package dev.schuberth.stan.model

/**
 * The type of booking, based on OFX transaction types.
 */
enum class BookingType(val description: String) {
    ATM("ATM debit or credit"),
    CASH("Cash withdrawal"),
    CHECK("Check"),
    CREDIT("Generic credit"),
    DEBIT("Generic debit"),
    DEP("Deposit"),
    DIRECTDEBIT("Merchant initiated debit"),
    DIRECTDEP("Direct deposit"),
    DIV("Dividend"),
    FEE("FI fee"),
    HOLD("Amount is under a hold"),
    INT("Interest earned or paid"),
    PAYMENT("Electronic payment"),
    POS("Point of sale debit or credit"),
    REPEATPMT("Repeating payment/standing order"),
    SRVCHG("Service charge"),
    XFER("Transfer"),

    OTHER("Other")
}
