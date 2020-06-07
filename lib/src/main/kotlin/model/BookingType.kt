package dev.schuberth.stan.model

/**
 * The type of booking, loosely based on OFX transaction types, see
 * https://www.ofx.net/downloads/OFX%202.2.pdf
 */
enum class BookingType {
    /**
     * An unknown type of booking. If this occurs the mapping might need an update or a new type be added.
     */
    UNKNOWN,

    /**
     * ATM debit or credit.
     */
    ATM,

    /**
     * Cash withdrawal.
     */
    CASH,

    /**
     * Check debit or credit.
     */
    CHECK,

    /**
     * Genric credit.
     */
    CREDIT,

    /**
     * Generic debit.
     */
    DEBIT,

    /**
     * Interest earned or paid.
     */
    INT,

    /**
     * Electronic payment.
     */
    PAYMENT,

    /**
     * Repeating payment/standing order.
     */
    REPEATPMT,

    /**
     * Salary debit.
     */
    SALARY,

    /**
     * Money transfer.
     */
    TRANSFER
}
