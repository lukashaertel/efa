package eu.pazuzu.eurofurence.efa.tools

import java.lang.Math.round

/**
 * To string as a percentage.
 */
val Double.percent get() = "${round(this * 1000.0) / 10.0}%"

/**
 * Rounds to one digit after period.
 */
val Double.round1 get() = "${round(this * 10.0) / 10.0}"

/**
 * Round to two digit after period.
 */
val Double.round2 get() = "${round(this * 100.0) / 100.0}"

/**
 * Matches Roman numerals.
 */
val romanNumeralsRegex = Regex("""M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})""")