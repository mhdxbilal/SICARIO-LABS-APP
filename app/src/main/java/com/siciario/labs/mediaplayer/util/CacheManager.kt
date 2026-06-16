package com.siciario.labs.mediaplayer.util

/**
 * Cache Manager - Manages local caching of metadata and thumbnails
 * Offline-first approach with local storage only
 */
class CacheManager {
    
    companion object {
        private const val CACHE_EXPIRY_MS = 30 * 24 * 60 * 60 * 1000L // 30 days
        private const val MAX_CACHE_SIZE = 500 * 1024 * 1024 // 500 MB
    }
    
    /**
     * Cache entry for metadata
     */
    data class CacheEntry(
        val key: String,
        val data: ByteArray,
        val timestamp: Long = System.currentTimeMillis(),
        val ttl: Long = CACHE_EXPIRY_MS
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > ttl
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CacheEntry) return false
            
            if (key != other.key) return false
            if (!data.contentEquals(other.data)) return false
            if (timestamp != other.timestamp) return false
            if (ttl != other.ttl) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + data.contentHashCode()
            result = 31 * result + timestamp.hashCode()
            result = 31 * result + ttl.hashCode()
            return result
        }
    }
    
    private val cache = mutableMapOf<String, CacheEntry>()
    private var totalCacheSize = 0L
    
    /**
     * Get cache entry by key
     */
    fun get(key: String): ByteArray? {
        val entry = cache[key] ?: return null
        
        return if (entry.isExpired()) {
            cache.remove(key)
            totalCacheSize -= entry.data.size
            null
        } else {
            entry.data
        }
    }
    
    /**
     * Put data in cache
     */
    fun put(key: String, data: ByteArray, ttl: Long = CACHE_EXPIRY_MS) {
        // Remove old entry if exists
        cache[key]?.let { oldEntry ->
            totalCacheSize -= oldEntry.data.size
        }
        
        val entry = CacheEntry(key, data, ttl = ttl)
        cache[key] = entry
        totalCacheSize += data.size
        
        // Cleanup if cache exceeds max size
        if (totalCacheSize > MAX_CACHE_SIZE) {
            cleanup()
        }
    }
    
    /**
     * Remove entry from cache
     */
    fun remove(key: String) {
        cache[key]?.let { entry ->
            totalCacheSize -= entry.data.size
        }
        cache.remove(key)
    }
    
    /**
     * Clear entire cache
     */
    fun clear() {
        cache.clear()
        totalCacheSize = 0
    }
    
    /**
     * Cleanup expired entries and oldest entries if cache exceeds max size
     */
    private fun cleanup() {
        // Remove expired entries
        cache.entries.removeAll { (_, entry) -> entry.isExpired() }
        
        // If still too large, remove oldest entries
        if (totalCacheSize > MAX_CACHE_SIZE) {
            val sortedByTime = cache.entries.sortedBy { it.value.timestamp }
            var removed = 0
            
            for (entry in sortedByTime) {
                if (totalCacheSize <= MAX_CACHE_SIZE * 0.8) break
                totalCacheSize -= entry.value.data.size
                cache.remove(entry.key)
                removed++
            }
        }
    }
    
    fun getCacheSize(): Long = totalCacheSize
}
