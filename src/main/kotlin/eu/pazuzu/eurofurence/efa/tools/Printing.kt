package eu.pazuzu.eurofurence.efa.tools

/**
 * Prints the items of the iterable in a table, padding the left side of entries.
 */
fun tabularize(values: Iterable<Any?>, columns: Int) {
    val strings = values.map(Any?::toString)
    val lengths = IntArray(columns)
    for ((i, v) in strings.withIndex())
        lengths[i % columns] = maxOf(lengths[i % columns], v.length)

    var properTermination = true
    for ((i, v) in strings.withIndex()) {
        properTermination = false

        print(v.padStart(lengths[i % columns]))
        when {
            i % columns < columns - 1 -> print(" ")
            i % columns == columns - 1 -> {
                println()
                properTermination = true
            }
        }
    }

    if (!properTermination)
        println()
}