package dev.schuberth.stan.utils

import java.util.ServiceLoader

inline fun <reified T : Any> getLoaderFor(): ServiceLoader<T> = ServiceLoader.load(T::class.java)

interface NamedPlugin {
    companion object {
        inline fun <reified T : NamedPlugin> getAll() = getLoaderFor<T>()
            .iterator()
            .asSequence()
            .associateByTo(sortedMapOf(String.CASE_INSENSITIVE_ORDER)) {
                it.name
            }
    }

    val name: String
}
