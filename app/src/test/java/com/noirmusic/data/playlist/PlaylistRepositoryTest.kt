package com.noirmusic.data.playlist

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PlaylistRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repo: JsonPlaylistRepository
    private lateinit var dummyContext: Context

    @Before
    fun setUp() {
        val root = tempFolder.newFolder("files")
        dummyContext = object : android.content.ContextWrapper(null) {
            override fun getFilesDir(): File = root
        }
        repo = JsonPlaylistRepository(dummyContext, fileName = "test_playlists.json")
    }

    @Test
    fun createPlaylistAndPersist() = runTest {
        assertTrue(repo.playlists.value.isEmpty())

        val created = repo.createPlaylist("Chill Beats", initialTrackId = 42L)
        assertEquals("Chill Beats", created.name)
        assertEquals(listOf(42L), created.trackIds)
        assertEquals(1, repo.playlists.value.size)

        // Verify loaded again from disk
        val repo2 = JsonPlaylistRepository(dummyContext, fileName = "test_playlists.json")
        assertEquals(1, repo2.playlists.value.size)
        assertEquals("Chill Beats", repo2.playlists.value[0].name)
        assertEquals(listOf(42L), repo2.playlists.value[0].trackIds)
    }

    @Test
    fun addAndRemoveTracks() = runTest {
        val p = repo.createPlaylist("Workout")

        val added1 = repo.addTrackToPlaylist(p.id, 101L)
        assertTrue(added1)
        assertEquals(listOf(101L), repo.playlists.value.first().trackIds)

        // Adding duplicate should return false and not duplicate
        val addedDup = repo.addTrackToPlaylist(p.id, 101L)
        assertFalse(addedDup)
        assertEquals(listOf(101L), repo.playlists.value.first().trackIds)

        repo.addTrackToPlaylist(p.id, 102L)
        assertEquals(listOf(101L, 102L), repo.playlists.value.first().trackIds)

        repo.removeTrackFromPlaylist(p.id, 101L)
        assertEquals(listOf(102L), repo.playlists.value.first().trackIds)
    }

    @Test
    fun renameAndDeletePlaylist() = runTest {
        val p1 = repo.createPlaylist("Old Name")
        repo.renamePlaylist(p1.id, "New Name")
        assertEquals("New Name", repo.playlists.value.first().name)

        repo.deletePlaylist(p1.id)
        assertTrue(repo.playlists.value.isEmpty())
    }
}
