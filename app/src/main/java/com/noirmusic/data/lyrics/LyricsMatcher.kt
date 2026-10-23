package com.noirmusic.data.lyrics

import kotlin.math.abs

/** Validates and ranks LRCLIB records against the local track. Pure. */
object LyricsMatcher {

    private const val DURATION_TOLERANCE_SEC = 3.0
    private const val STRICT_DURATION_TOLERANCE_SEC = 2.0

    private fun normTitle(s: String?): String = TextNormalizer.normalize(TextNormalizer.cleanTitle(s.orEmpty()))

    fun titleMatches(local: String?, remote: String?): Boolean {
        val a = normTitle(local)
        val b = normTitle(remote)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        val ta = a.split(' ').toSet()
        val tb = b.split(' ').toSet()
        val jaccard = ta.intersect(tb).size.toDouble() / ta.union(tb).size
        if (jaccard >= 0.75) return true
        val (shorter, longer) = if (a.length <= b.length) a to b else b to a
        return shorter.length >= 4 && longer.startsWith(shorter)
    }

    fun artistMatches(local: String?, remote: String?): Boolean {
        val a = TextNormalizer.normalize(local)
        val b = TextNormalizer.normalize(remote)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        val pa = TextNormalizer.splitArtists(local).toSet()
        val pb = TextNormalizer.splitArtists(remote).toSet()
        if (pa.intersect(pb).isNotEmpty()) return true
        val (shorter, longer) = if (a.length <= b.length) a to b else b to a
        return shorter.length >= 3 && longer.contains(shorter)
    }

    fun durationDiff(query: LyricsQuery, dto: LrclibRecordDto): Double? {
        val local = query.durationSec ?: return null
        val remote = dto.duration ?: return null
        return abs(local - remote)
    }

    private fun hasContent(dto: LrclibRecordDto): Boolean =
        dto.instrumental || !dto.syncedLyrics.isNullOrBlank() || !dto.plainLyrics.isNullOrBlank()

    fun isAcceptable(query: LyricsQuery, dto: LrclibRecordDto): Boolean {
        if (!hasContent(dto)) return false
        if (!titleMatches(query.title, dto.resolvedTrackName)) return false
        val diff = durationDiff(query, dto)
        return if (query.artist != null) {
            artistMatches(query.artist, dto.artistName) && (diff == null || diff <= DURATION_TOLERANCE_SEC)
        } else {
            diff != null && diff <= STRICT_DURATION_TOLERANCE_SEC &&
                normTitle(query.title) == normTitle(dto.resolvedTrackName)
        }
    }

    fun selectBest(query: LyricsQuery, candidates: List<LrclibRecordDto>): LrclibRecordDto? {
        val acceptable = candidates.filter { isAcceptable(query, it) }
        if (acceptable.isEmpty()) return null
        val queryTitle = normTitle(query.title)
        val queryAlbum = TextNormalizer.normalize(query.album)
        val comparator = compareByDescending<LrclibRecordDto> { LrcParser.parse(it.syncedLyrics).isNotEmpty() }
            .thenBy { durationDiff(query, it) ?: Double.MAX_VALUE }
            .thenByDescending { normTitle(it.resolvedTrackName) == queryTitle }
            .thenByDescending { queryAlbum.isNotEmpty() && TextNormalizer.normalize(it.albumName) == queryAlbum }
            .thenBy { it.id ?: Long.MAX_VALUE }
        val withLyrics = acceptable.filter { !it.instrumental }
        return (withLyrics.ifEmpty { acceptable }).sortedWith(comparator).first()
    }

    /** Converts a validated record into a [LyricsResult]. */
    fun toResult(dto: LrclibRecordDto): LyricsResult {
        if (dto.instrumental) return LyricsResult.Instrumental
        val synced = LrcParser.parse(dto.syncedLyrics)
        if (synced.isNotEmpty()) return LyricsResult.Synced(synced, dto.plainLyrics)
        val plain = LrcParser.plainLines(dto.plainLyrics)
        if (plain.isNotEmpty()) return LyricsResult.Plain(plain)
        return LyricsResult.NotFound
    }
}
