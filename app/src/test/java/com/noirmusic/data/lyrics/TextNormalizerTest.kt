package com.noirmusic.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun normalizeFoldsDiacriticsAmpersandFeaturingPunctuationAndLeadingThe() {
        assertEquals("beyonce", TextNormalizer.normalize("  Beyoncé! "))
        assertEquals("simon and garfunkel", TextNormalizer.normalize("The Simon & Garfunkel"))
        assertEquals("get lucky", TextNormalizer.normalize("Get Lucky (feat. Pharrell Williams)"))
        assertEquals("get lucky", TextNormalizer.normalize("Get Lucky ft. Pharrell"))
        assertEquals("ac dc", TextNormalizer.normalize("AC/DC —  "))
    }

    @Test
    fun cleanTitleStripsNoiseAnnotationsAndPreservesVersionIdentity() {
        assertEquals("Heroes", TextNormalizer.cleanTitle("Heroes (2017 Remaster)"))
        assertEquals("Heroes", TextNormalizer.cleanTitle("Heroes [Remastered]"))
        assertEquals("Heroes", TextNormalizer.cleanTitle("Heroes - 2011 Remaster"))
        assertEquals("Heroes", TextNormalizer.cleanTitle("Heroes - Radio Edit"))
        assertEquals("Heroes", TextNormalizer.cleanTitle("Heroes (feat. Someone)"))
        assertEquals("Heroes (Live)", TextNormalizer.cleanTitle("Heroes (Live) - Remastered 2020"))
        assertEquals("(Remastered)", TextNormalizer.cleanTitle("(Remastered)"))
    }

    @Test
    fun splitArtistsAndUnknownArtistDetection() {
        assertEquals(
            listOf("daft punk", "pharrell williams", "nile rodgers"),
            TextNormalizer.splitArtists("Daft Punk & Pharrell Williams feat. Nile Rodgers"),
        )
        assertTrue(TextNormalizer.isUnknownArtist("<unknown>"))
        assertTrue(TextNormalizer.isUnknownArtist("Unknown Artist"))
        assertTrue(TextNormalizer.isUnknownArtist("   "))
        assertTrue(TextNormalizer.isUnknownArtist(null))
        assertFalse(TextNormalizer.isUnknownArtist("Portishead"))
    }
}
