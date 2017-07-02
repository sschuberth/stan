package com.github.sschuberth.stan.functionaltest

import java.io.File

internal object Resources

/**
 * A helper function to return the content of a resource text file as a string.
 *
 * @param path The path to the resource file.
 * @return The text file content as a strings.
 */
fun readResource(path: String): String {
    return File(Resources.javaClass.getResource(path).toURI()).readText()
}
