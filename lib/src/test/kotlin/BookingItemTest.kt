package dev.schuberth.stan

import dev.schuberth.stan.model.BookingItem
import dev.schuberth.stan.model.BookingType

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import java.time.LocalDate

class BookingItemTest : StringSpec({
    "Info lines should be joined correctly" {
        val item = BookingItem(
            postDate = LocalDate.EPOCH,
            valueDate = LocalDate.EPOCH,
            info = mutableListOf(
                "PRIVATHAFTPFLICHT-",
                "VERS.",
                "Einreicher-",
                "ID",
                "Telto-",
                "wer Damm"
            ),
            amount = Float.NaN,
            type = BookingType.OTHER
        )

        item.joinInfo() shouldBe "PRIVATHAFTPFLICHT-VERS., Einreicher-ID, Teltower Damm"
        item.joinInfo(" / ") shouldBe "PRIVATHAFTPFLICHT-VERS. / Einreicher-ID / Teltower Damm"
    }
})