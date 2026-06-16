package com.siciario.labs.mediaplayer.feature

import com.siciario.labs.mediaplayer.data.model.MediaItem

/**
 * Media Filter - Offline media filtering and organization
 */
class MediaFilter {
    
    fun filterByGenre(mediaItems: List<MediaItem>, genre: String): List<MediaItem> {
        return mediaItems.filter { it.genre?.lowercase() == genre.lowercase() }
    }
    
    fun filterByArtist(mediaItems: List<MediaItem>, artist: String): List<MediaItem> {
        return mediaItems.filter { it.artist?.lowercase() == artist.lowercase() }
    }
    
    fun filterByAlbum(mediaItems: List<MediaItem>, album: String): List<MediaItem> {
        return mediaItems.filter { it.album?.lowercase() == album.lowercase() }
    }
    
    fun filterByType(mediaItems: List<MediaItem>, mimeTypePrefix: String): List<MediaItem> {
        return mediaItems.filter { it.mimeType.startsWith(mimeTypePrefix) }
    }
    
    fun sortByTitle(mediaItems: List<MediaItem>): List<MediaItem> {
        return mediaItems.sortedBy { it.title ?: it.fileName }
    }
    
    fun sortByArtist(mediaItems: List<MediaItem>): List<MediaItem> {
        return mediaItems.sortedBy { it.artist ?: "" }
    }
    
    fun sortByDateAdded(mediaItems: List<MediaItem>, descending: Boolean = true): List<MediaItem> {
        return if (descending) {
            mediaItems.sortedByDescending { it.dateAdded }
        } else {
            mediaItems.sortedBy { it.dateAdded }
        }
    }
    
    fun sortByPlayCount(mediaItems: List<MediaItem>, descending: Boolean = true): List<MediaItem> {
        return if (descending) {
            mediaItems.sortedByDescending { it.playCount }
        } else {
            mediaItems.sortedBy { it.playCount }
        }
    }
    
    fun getGenres(mediaItems: List<MediaItem>): Set<String> {
        return mediaItems.mapNotNull { it.genre }.toSet()
    }
    
    fun getArtists(mediaItems: List<MediaItem>): Set<String> {
        return mediaItems.mapNotNull { it.artist }.toSet()
    }
    
    fun getAlbums(mediaItems: List<MediaItem>): Set<String> {
        return mediaItems.mapNotNull { it.album }.toSet()
    }
}
