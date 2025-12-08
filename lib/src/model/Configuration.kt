package dev.schuberth.stan.model

import java.io.File

interface Configuration {
    fun getStatementFiles(): Set<File>

    fun findBookingCategory(item: BookingItem): BookingCategory?
}
