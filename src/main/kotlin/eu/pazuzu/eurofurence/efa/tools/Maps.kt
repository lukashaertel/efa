package eu.pazuzu.eurofurence.efa.tools

/**
 * `Distinct by` for maps. Uses [Map.entries] and [associate] to wrap the distinct selection.
 */
inline fun <K, V, R> Map<K, V>.distinctBy(select: (Map.Entry<K, V>) -> R) =
        entries.distinctBy(select).associate(Map.Entry<K, V>::toPair)