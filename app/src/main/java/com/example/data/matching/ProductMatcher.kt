package com.example.data.matching

import java.text.Normalizer

/**
 * Lokale Zuordnungs-Hilfen: EAN-Erkennung, Suchbegriff aus Titel, Titel-Ähnlichkeit.
 */
object ProductMatcher {

    private val stopWords = setOf(
        // deutsch
        "und", "oder", "mit", "für", "fur", "fuer", "von", "der", "die", "das", "den", "dem", "des", "ein", "eine",
        "einer", "eines", "im", "in", "am", "an", "auf", "aus", "zu", "zum", "zur", "bei", "neu", "neue", "neuer",
        "neues", "stück", "stuck", "stk", "set", "heißer", "heisser", "verkauf", "angebot", "qualität", "qualitat",
        "hohe", "hoher", "hochwertige", "hochwertig", "kostenloser", "versand", "original", "top", "beste", "besten",
        "universal", "universelle", "universeller", "neueste", "mode", "modische",
        // englisch
        "and", "or", "with", "for", "the", "a", "an", "of", "to", "in", "on", "new", "hot", "sale", "pcs", "pc",
        "piece", "pieces", "high", "quality", "free", "shipping", "best", "fashion", "style", "latest"
    )

    private val gtinRegex = Regex("(?<!\\d)(\\d{13}|\\d{12}|\\d{8})(?!\\d)")
    private val yearRegex = Regex("^(19|20)\\d{2}$")

    /** Sucht eine gültige EAN-13 / UPC-12 / EAN-8 im Text (mit Prüfziffer). */
    fun extractGtin(text: String): String? {
        return gtinRegex.findAll(text)
            .map { it.value }
            .firstOrNull { isValidGtin(it) }
    }

    fun isValidGtin(code: String): Boolean {
        if (code.length !in setOf(8, 12, 13, 14) || code.any { !it.isDigit() }) return false
        val digits = code.map { it - '0' }
        val check = digits.last()
        val body = digits.dropLast(1).reversed()
        var sum = 0
        body.forEachIndexed { i, d -> sum += if (i % 2 == 0) d * 3 else d }
        return (10 - (sum % 10)) % 10 == check
    }

    fun tokenize(text: String): List<String> {
        val lowered = text.lowercase()
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
        val normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return normalized
            .replace(Regex("[\\[\\](){}]"), " ")
            .split(Regex("[^a-z0-9.]+"))
            .map { it.trim('.') }
            .filter { it.length >= 2 && it !in stopWords && !yearRegex.matches(it) }
    }

    /** Kompakter eBay-Suchbegriff aus einem langen AliExpress-Titel. */
    fun buildSearchQuery(title: String, maxWords: Int = 7): String {
        val cleaned = title.replace(Regex("[\\[(][^\\])]*[\\])]"), " ")
        val seen = LinkedHashSet<String>()
        cleaned.split(Regex("[\\s,;/|+]+"))
            .map { it.trim(' ', '-', '.', '!', '?', ':', '"', '\'') }
            .filter { it.length >= 2 }
            .filter { tokenize(it).isNotEmpty() }
            .forEach { word ->
                if (seen.none { it.equals(word, ignoreCase = true) }) seen.add(word)
            }
        return seen.take(maxWords).joinToString(" ")
    }

    /**
     * Titel-Ähnlichkeit 0-100: gewichtete Token-Überdeckung plus Bonus/Malus für Zahlen-Angaben
     * (Maße, Mengen, Kapazitäten wie "5m", "64gb", "2x").
     */
    fun titleSimilarity(a: String, b: String): Int {
        val ta = tokenize(a).toSet()
        val tb = tokenize(b).toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0

        val common = ta.intersect(tb)
        val jaccard = common.size.toDouble() / ta.union(tb).size.toDouble()
        val coverage = common.size.toDouble() / minOf(ta.size, tb.size).toDouble()
        var score = (0.4 * jaccard + 0.6 * coverage) * 100.0

        val numA = ta.filter { t -> t.any { it.isDigit() } }.toSet()
        val numB = tb.filter { t -> t.any { it.isDigit() } }.toSet()
        if (numA.isNotEmpty() && numB.isNotEmpty()) {
            score += if (numA.intersect(numB).isNotEmpty()) 8.0 else -12.0
        }
        return score.toInt().coerceIn(0, 100)
    }
}
