package eu.pazuzu.eurofurence.efa

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import eu.pazuzu.eurofurence.efa.tools.*
import org.tartarus.martin.Stemmer
import java.awt.Desktop
import java.io.File
import java.io.PrintStream
import java.lang.Math.*

/**
 * Returns a lowercase string consisting of characters.
 */
fun String.normalize() =
        toLowerCase().filter(Char::isLetter)

/**
 * True if string is a stopword.
 */
fun String.isStopword() = this in stopwords

/**
 * True if string is a roman numeral.
 */
fun String.isRomanNumeral() = romanNumeralsRegex.matches(this)

/**
 * Stems the string.
 */
fun String.stem(): String {
    val stemmer = Stemmer()
    val chars = toCharArray()
    stemmer.add(chars, chars.size)
    stemmer.stem()
    return String(stemmer.resultBuffer, 0, stemmer.resultLength)
}

/**
 * Vectorizes the document.
 */
fun vectorize(string: String) =
        string.splitToSequence(".", "-", "&", "!", " ", "\r", "\n", "\t")
                .map(String::normalize)
                .filterNot(String::isBlank)
                .filterNot(String::isStopword)
                .filterNot(String::isRomanNumeral)
                .map(String::stem)
                .toList()
                .let { ws ->
                    ws.distinct().associate { w -> w to ws.count { w == it } }
                }

/**
 * Applies TF-IDF on a collection of documents.
 */
fun <Id> tfIdfVectors(docs: Map<Id, Map<String, Int>>): Map<Id, Map<String, Double>> {
    // IDF function, relative to documents
    fun idf(w: String) =
            log((docs.size / docs.values.count { w in it.keys }).toDouble())

    return docs.mapValues { (_, d) ->
        // Maximum term frequency, relative to document
        val mdf = d.values.max()!!.toDouble()

        // Adjusted term frequency, relative to document
        fun tf(c: Int) =
                0.5 + 0.5 * c.toDouble() / mdf

        // Map to TF-IDF
        d.mapValues { (w, c) ->
            tf(c) * idf(w)
        }
    }
}

/**
 * Calculates the length of the document vector.
 */
fun len(x: Iterable<Double>) = sqrt(x.map { it * it }.sum())

/**
 * Calculates cosine similarity on the two document vectors.
 */
fun sim(a: Map<String, Double>, b: Map<String, Double>) =
        a.mapValues { (w, c) ->
            c * b.getOrDefault(w, 0.0)
        }.values.sum() / len(a.values) / len(b.values)

/**
 * Sum of values bound to the same keys.
 */
infix fun <T> Map<T, Int>.docSum(other: Map<T, Int>) =
        (keys + other.keys).associate {
            it to (getOrDefault(it, 0) + other.getOrDefault(it, 0))
        }


fun main(args: Array<String>) {

    // Read track entry
    val rawTrack = Parser().parse(resource("EventConferenceTrack")).safeAs<JsonObject>()

    // Interpret by plain association
    val dataTrack = rawTrack.associate {
        val i = it["Id"] as String
        val n = it["Name"] as String
        i to n
    }

    // Configuration (TODO: since TF-IDF is used, multiple occurrences decrease value)
    val weightTitle = 1
    val weightTrack = 1
    val weightAbstract = 1
    val weightDescription = 1

    // Read event entries
    val rawEntry = Parser().parse(resource("EventEntry")).safeAs<JsonObject>()

    // Interpret by vectorization
    val dataEntry = rawEntry.associate {
        val i = it["Title"] as String
        val a = it["Abstract"] as String
        val d = it["Description"] as String
        val t = dataTrack[it["ConferenceTrackId"] as String]!!

        // Print event-track binding
        println(">> $i in $t")

        // Vectorize and apply weights
        val di = vectorize(i).mapValues { (_, v) -> v * weightTitle }
        val da = vectorize(a).mapValues { (_, v) -> v * weightAbstract }
        val dd = vectorize(d).mapValues { (_, v) -> v * weightDescription }
        val dt = vectorize(t).mapValues { (_, v) -> v * weightTrack }

        // Make final document
        val doc = di docSum da docSum dd docSum dt

        // Associate
        i to doc
    }.distinctBy { (_, v) -> v }

    // Apply TF-IDF
    val dataTfIdf = tfIdfVectors(dataEntry)

    for ((t, d) in dataTfIdf) {
        // Get the most similar documents
        val similar = dataTfIdf
                .filterKeys { t != it }
                .mapValues { (_, v) -> sim(d, v) }
                .entries
                .sortedByDescending { (_, v) -> v }
                .take(5)

        // Get the scores but prettyfy by ordering and rounding
        val prettyScores = d
                .entries
                .sortedByDescending { (_, v) -> v }
                .map { (k, v) -> "$k: ${v.round2}" }

        // Print output
        println(t)
        for ((o, s) in similar)
            println("  See also: $o (s=${s.percent})")
        println("TF-IDF scores:")

        tabularize(prettyScores, 8)
        println()
    }

    // Configuration of graphs
    val minSim = 0.08
    val lineScale = 0.75

    // Print graph
    PrintStream("map.dot").useApply {
        // Print header
        println("graph map {")
        println("graph [layout=\"sfdp\" overlap=prism];")

        // Calculate the maximum inwards similarity for all nodes
        val maxInSim = dataTfIdf.values
                .map { d1 -> dataTfIdf.values.map { d2 -> sim(d1, d2) }.sum() }.max()!!

        // Take all documents
        for ((i1, d1) in dataTfIdf.entries) {
            val inSim = dataTfIdf.values.map { sim(d1, it) }.sum() / maxInSim
            // Print a node for the given item
            println("\"$i1\" [shape=box style=filled fillcolor=\"$inSim,0.5,1\"];")

            // Take the following documents
            for ((i2, d2) in dataTfIdf
                    .entries
                    .dropWhile { (i, _) -> i1 != i }
                    .drop(1)) {

                // Print an edge if similarity surpasses the threshold, increase the number of printed edges
                val s = sim(d1, d2)
                if (s > minSim)
                    println("\"$i1\" -- \"$i2\" [weight=$s penwidth=${lineScale * s / minSim} color=\"$s,1,1\"];")
            }
        }

        // Close
        println("}")
    }

    // Start process of converting DOT to PNG
    ProcessBuilder()
            .command("dot", "-Tpng", "-o", "map.png", "map.dot")
            .inheritIO()
            .start()
            .waitFor()

    // Delete temporary dot file
    File("map.dot").delete()
}