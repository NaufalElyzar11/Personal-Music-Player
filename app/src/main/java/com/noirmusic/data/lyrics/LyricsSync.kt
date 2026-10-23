package com.noirmusic.data.lyrics

object LyricsSync {
    /**
     * Index of the last line whose timestamp is <= [positionMs], or -1 before the first line / for an empty list.
     * Among equal timestamps the last one wins. O(log n).
     */
    fun indexAt(lines: List<LyricLine>, positionMs: Long): Int {
        var lo = 0
        var hi = lines.size - 1
        var result = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return result
    }
}
