package com.noirmusic.data.lyrics

import java.text.Normalizer
import java.util.Locale

/** Pure text normalization used for search, matching and cache keys. */
object TextNormalizer {

    private val COMBINING_MARKS = Regex("\\p{M}+")
    private val FEATURING_TAIL = Regex("(?i)[\\s(\\[]+(feat\\.?|ft\\.?|featuring)\\s.*$")
    private val NON_ALNUM = Regex("[^\\p{L}\\p{N}]+")
    private val WHITESPACE = Regex("\\s+")
    private val BRACKET_GROUP = Regex("\\s*[(\\[]([^)\\]]*)[)\\]]")
    private val NOISE_WORDS = Regex(
        "(?i)\\b(feat\\.?|ft\\.?|featuring|remaster(ed)?|\\d{4} remaster(ed)?|radio edit|single version|" +
            "album version|explicit|clean|mono|stereo|bonus track)(?=\\W|$)",
    )
    private val DASH_SUFFIX = Regex(
        "(?i)\\s+-\\s+(\\d{4}\\s+)?(remaster(ed)?|radio edit|single version|mono|stereo)(\\s+\\d{4})?(\\s+version)?\\s*$",
    )
    private val ARTIST_SEPARATORS = Regex(
        "(?i)\\s*(,|;|/|&|\\+|\\sx\\s|\\sfeat\\.?\\s|\\sft\\.?\\s|\\sfeaturing\\s|\\sand\\s|\\swith\\s)\\s*",
    )
    private val UNKNOWN_ARTISTS = setOf("<unknown>", "unknown", "unknown artist")

    private fun foldDiacritics(s: String): String =
        COMBINING_MARKS.replace(Normalizer.normalize(s, Normalizer.Form.NFKD), "")

    /** Lowercase, diacritic-free, punctuation-free form for comparing titles and artists. */
    fun normalize(s: String?): String {
        if (s.isNullOrBlank()) return ""
        var t = foldDiacritics(s).lowercase(Locale.ROOT)
        t = t.replace("&", " and ")
        t = FEATURING_TAIL.replace(t, "")
        t = NON_ALNUM.replace(t, " ")
        t = WHITESPACE.replace(t, " ").trim()
        if (t.startsWith("the ")) t = t.removePrefix("the ").trim()
        return t
    }

    /** Lighter normalization for the library search box (keeps "the", no feat removal). */
    fun normalizeForSearch(s: String?): String {
        if (s.isNullOrBlank()) return ""
        return WHITESPACE.replace(foldDiacritics(s).lowercase(Locale.ROOT), " ").trim()
    }

    /** Removes remaster/featuring/edit decorations from a title; keeps e.g. "(Live)". */
    fun cleanTitle(s: String): String {
        val original = s.trim()
        var t = BRACKET_GROUP.replace(original) { m ->
            if (NOISE_WORDS.containsMatchIn(m.groupValues[1])) "" else m.value
        }
        t = DASH_SUFFIX.replace(t, "")
        t = WHITESPACE.replace(t, " ").trim()
        return t.ifEmpty { original }
    }

    /** Splits a multi-artist credit into normalized, de-duplicated parts. */
    fun splitArtists(s: String?): List<String> {
        if (s.isNullOrBlank()) return emptyList()
        return s.split(ARTIST_SEPARATORS)
            .map { normalize(it) }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun isUnknownArtist(s: String?): Boolean =
        s == null || s.isBlank() || s.trim().lowercase(Locale.ROOT) in UNKNOWN_ARTISTS
}
