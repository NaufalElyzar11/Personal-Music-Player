package com.noirmusic.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parsesTimestampVariants() {
        val lrc = """
            [01:02.34]Two digit fraction
            [01:02]No fraction
            [01:02.3]One digit fraction
            [01:02.345]Three digit fraction
            [01:02:34]Colon fraction
            [100:00.00]Three digit minutes
        """.trimIndent()

        val lines = LrcParser.parse(lrc)
        assertEquals(
            listOf(
                LyricLine(62_000, "No fraction"),
                LyricLine(62_300, "One digit fraction"),
                LyricLine(62_340, "Two digit fraction"),
                LyricLine(62_340, "Colon fraction"),
                LyricLine(62_345, "Three digit fraction"),
                LyricLine(6_000_000, "Three digit minutes"),
            ),
            lines,
        )
    }

    @Test
    fun expandsMultiTimestampLinesAndSortsStably() {
        val lrc = """
            [00:10.00][00:30.00]Chorus line
            [00:20.00]Verse line
            [00:10.00]Second at ten
        """.trimIndent()

        val lines = LrcParser.parse(lrc)
        assertEquals(
            listOf(
                LyricLine(10_000, "Chorus line"),
                LyricLine(10_000, "Second at ten"),
                LyricLine(20_000, "Verse line"),
                LyricLine(30_000, "Chorus line"),
            ),
            lines,
        )
    }

    @Test
    fun ignoresMetadataAndAppliesPositiveAndNegativeOffsetClampedAtZero() {
        val positive = """
            [ar:Someone]
            [ti:Song]
            [al:Album]
            [by:Creator]
            [length:03:00]
            [offset:+500]
            [00:00.20]Clamped
            [00:02.00]Shifted earlier
        """.trimIndent()
        assertEquals(
            listOf(
                LyricLine(0, "Clamped"),
                LyricLine(1_500, "Shifted earlier"),
            ),
            LrcParser.parse(positive),
        )

        val negative = """
            [offset:-500]
            [00:02.00]Shifted later
        """.trimIndent()
        assertEquals(listOf(LyricLine(2_500, "Shifted later")), LrcParser.parse(negative))
    }

    @Test
    fun keepsBlankTimedLinesAndStripsInlineWordTimestamps() {
        val lrc = "\uFEFF[00:01.00]<00:01.10>Hello <00:01.50>world\r\n[00:05.00]\r\n[00:08.00]End"
        assertEquals(
            listOf(
                LyricLine(1_000, "Hello world"),
                LyricLine(5_000, ""),
                LyricLine(8_000, "End"),
            ),
            LrcParser.parse(lrc),
        )
    }

    @Test
    fun ignoresMalformedLinesAndTreatsAllBlankLyricsAsAbsent() {
        val lrc = """
            [1:75.00]Invalid seconds over 59
            [00:60.00]Invalid exactly 60 seconds
            [chorus]
            Not a tag at all
            [00:04.00]Valid line
        """.trimIndent()
        assertEquals(listOf(LyricLine(4_000, "Valid line")), LrcParser.parse(lrc))

        assertTrue(LrcParser.parse("").isEmpty())
        assertTrue(LrcParser.parse(null).isEmpty())
        assertTrue(LrcParser.parse("[00:01.00]\n[00:02.00]   ").isEmpty())
    }

    @Test
    fun handlesWhitespaceBetweenTimestampTags() {
        val lrc = "[00:05.00]   [00:15.50]  Repeated chorus line"
        val parsed = LrcParser.parse(lrc)
        assertEquals(
            listOf(
                LyricLine(5_000, "Repeated chorus line"),
                LyricLine(15_500, "Repeated chorus line"),
            ),
            parsed,
        )
    }

    @Test
    fun plainLinesTrimsOuterBlanksAndPreservesInnerSpacing() {
        val raw = "\uFEFF\n  \r\nFirst line  \r\n   \r\nSecond line\n\n"
        assertEquals(listOf("First line", "", "Second line"), LrcParser.plainLines(raw))
        assertTrue(LrcParser.plainLines("   \n  ").isEmpty())
        assertTrue(LrcParser.plainLines(null).isEmpty())
    }
}
