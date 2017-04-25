package eu.pazuzu.eurofurence.efa.tools

import java.io.Closeable
import java.io.File
import java.io.InputStreamReader

/**
 * Opens a resource from the compiled output.
 */
fun resource(path: String) =
        InputStreamReader(Thread.currentThread().contextClassLoader.getResourceAsStream(path))

/**
 * Uses but with an [apply] block.
 */
inline fun <T : Closeable?> T.useApply(block: T.() -> Unit) =
        use { it.apply(block) }