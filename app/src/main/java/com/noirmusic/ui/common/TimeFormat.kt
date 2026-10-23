package com.noirmusic.ui.common

import java.util.Locale

/** `m:ss`, or `h:mm:ss` from one hour; negative/unknown → `--:--`. */
fun formatDuration(ms: Long): String {
    if (ms < 0) return "--:--"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.ROOT, "%d:%02d", m, s)
    }
}

/** Spoken form for accessibility, e.g. "3 minutes 45 seconds". */
fun spokenDuration(ms: Long): String {
    if (ms <= 0) return "unknown duration"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return buildList {
        if (h > 0) add("$h ${if (h == 1L) "hour" else "hours"}")
        if (m > 0) add("$m ${if (m == 1L) "minute" else "minutes"}")
        if (s > 0 || (h == 0L && m == 0L)) add("$s ${if (s == 1L) "second" else "seconds"}")
    }.joinToString(" ")
}
