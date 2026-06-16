package com.siciario.labs.mediaplayer.feature

import com.siciario.labs.mediaplayer.data.model.MediaItem
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Media Filter Tests - Unit tests for offline media filtering
 */
class MediaFilterTest {
    
    private lateinit var mediaFilter: MediaFilter
    private lateinit var testMediaItems: List<MediaItem>
    
    @Before
    fun setup() {
        mediaFilter = MediaFilter()
        testMediaItems = listOf(
            MediaItem(
                id = 1,
                filePath = "/test/song1.mp3",
                fileName = "song1.mp3",
                title = "Song 1",
                artist = "Artist A",
                album = "Album 1",
                genre = "Rock",
                duration = 200000,
                fileSize = 5000000,
                mimeType = "audio/mpeg",
                dateAdded = System.currentTimeMillis(),
                dateModified = System.currentTimeMillis()
            ),
            MediaItem(
                id = 2,
                filePath = "/test/song2.mp3",
                fileName = "song2.mp3",
                title = "Song 2",
                artist = "Artist B",
                album = "Album 2",
                genre = "Pop",
                duration = 180000,
                fileSize = 4000000,
                mimeType = "audio/mpeg",
                dateAdded = System.currentTimeMillis(),
                dateModified = System.currentTimeMillis()
            )
        )
    }
    
    @Test
    fun testFilterByGenre() {
        val rockSongs = mediaFilter.filterByGenre(testMediaItems, "Rock")
        assertEquals(1, rockSongs.size)
        assertEquals("Rock", rockSongs[0].genre)
    }
    
    @Test
    fun testSortByTitle() {
        val sorted = mediaFilter.sortByTitle(testMediaItems)
        assertEquals("Song 1", sorted[0].title)
        assertEquals("Song 2", sorted[1].title)
    }
    
    @Test
    fun testGetGenres() {
        val genres = mediaFilter.getGenres(testMediaItems)
        assertEquals(2, genres.size)
        assert(genres.contains("Rock"))
        assert(genres.contains("Pop"))
    }
}
