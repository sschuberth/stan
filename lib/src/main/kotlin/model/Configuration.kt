package dev.schuberth.stan.model

interface Configuration {
    fun findBookingCategory(item: BookingItem): BookingCategory?
}
