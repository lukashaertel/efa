package eu.pazuzu.eurofurence.efa.tools

import com.beust.klaxon.JsonArray

/**
 * Performs a safe array cast.
 */
inline fun <reified T> Any?.safeAs(): JsonArray<T> {
    // Top level type mismatch
    if (this !is JsonArray<*>)
        throw IllegalArgumentException("Receiver not a JSON array")

    // Non-empty type mismatch
    if (!isEmpty() && get(0) !is T)
        throw IllegalArgumentException("Receiver not a JSON array of T")

    // Is actually checked cast
    @Suppress("UNCHECKED_CAST")
    return this as JsonArray<T>
}