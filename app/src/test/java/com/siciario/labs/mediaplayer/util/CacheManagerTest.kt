package com.siciario.labs.mediaplayer.util

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * CacheManager Tests - Unit tests for offline cache management
 */
class CacheManagerTest {
    
    private lateinit var cacheManager: CacheManager
    
    @Before
    fun setup() {
        cacheManager = CacheManager()
    }
    
    @Test
    fun testPutAndGet() {
        val testKey = "test_key"
        val testData = byteArrayOf(1, 2, 3, 4, 5)
        
        cacheManager.put(testKey, testData)
        val retrievedData = cacheManager.get(testKey)
        
        assertNotNull(retrievedData)
        assertTrue(retrievedData!!.contentEquals(testData))
    }
    
    @Test
    fun testRemove() {
        val testKey = "test_key"
        val testData = byteArrayOf(1, 2, 3, 4, 5)
        
        cacheManager.put(testKey, testData)
        cacheManager.remove(testKey)
        
        assertEquals(null, cacheManager.get(testKey))
    }
    
    @Test
    fun testClear() {
        cacheManager.put("key1", byteArrayOf(1, 2, 3))
        cacheManager.put("key2", byteArrayOf(4, 5, 6))
        
        cacheManager.clear()
        
        assertEquals(0, cacheManager.getCacheSize())
    }
}
