package com.noirmusic.data.lyrics

/** Robust LRC parser. Never throws; malformed input yields fewer or no lines. */
object LrcParser {

    private val LINE_SPLIT = Regex("\r\n|\n|\r")
    private val TIME_TAG = Regex("^\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
    private val META_TAG = Regex("^\\[([a-zA-Z#]+):(.*)]$")
    private val WORD_TAG = Regex("<\\d{1,3}:\\d{1,2}(?:[.:]\\d{1,3})?>")

    fun parse(lrc: String?): List<LyricLine> {
        if (lrc.isNullOrBlank()) return emptyList()
        val raw = mutableListOf<LyricLine>()
        var offsetMs = 0L

        for (rawLine in lrc.removePrefix("\uFEFF").split(LINE_SPLIT)) {
            var rest = rawLine.trim()
            if (rest.isEmpty()) continue

            val times = mutableListOf<Long>()
            var invalid = false
            while (true) {
                val m = TIME_TAG.find(rest) ?: break
                val minutes = m.groupValues[1].toLong()
                val seconds = m.groupValues[2].toLong()
                val fraction = m.groupValues[3]
                if (seconds > 59) {
                    invalid = true
                    break
                }
                val fractionMs = when (fraction.length) {
                    0 -> 0L
                    1 -> fraction.toLong() * 100
                    2 -> fraction.toLong() * 10
                    else -> fraction.toLong()
                }
                times += minutes * 60_000 + seconds * 1_000 + fractionMs
                rest = rest.substring(m.range.last + 1).trimStart()
            }
            if (invalid) continue

            if (times.isEmpty()) {
                val meta = META_TAG.find(rest)
                if (meta != null && meta.groupValues[1].equals("offset", ignoreCase = true)) {
                    meta.groupValues[2].trim().removePrefix("+").toLongOrNull()?.let { offsetMs = it }
                }
                continue // metadata, section markers and untimed text are ignored
            }

            val text = WORD_TAG.replace(rest, "").trim()
            times.forEach { raw += LyricLine(it, text) }
        }

        if (raw.none { it.text.isNotEmpty() }) return emptyList()
        return raw
            .map { it.copy(timeMs = (it.timeMs - offsetMs).coerceAtLeast(0L)) }
            .sortedBy { it.timeMs } // stable sort
    }

    /** Splits plain lyrics into lines, keeping inner blank lines and dropping leading/trailing blanks. */
    fun plainLines(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        val lines = text.removePrefix("\uFEFF").split(LINE_SPLIT).map { it.trimEnd() }
        val first = lines.indexOfFirst { it.isNotBlank() }
        val last = lines.indexOfLast { it.isNotBlank() }
        if (first < 0) return emptyList()
        return lines.subList(first, last + 1).map { if (it.isBlank()) "" else it }
    }
}
