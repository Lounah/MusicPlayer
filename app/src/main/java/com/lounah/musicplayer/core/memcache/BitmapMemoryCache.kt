package com.lounah.musicplayer.core.memcache

import android.graphics.Bitmap
import android.util.Log
import java.util.*
import kotlin.collections.LinkedHashMap

class BitmapMemoryCache private constructor() {
    private object Holder {
        val INSTANCE = BitmapMemoryCache()
    }

    companion object {
        val instance: BitmapMemoryCache by lazy { Holder.INSTANCE }
    }

    private val cache = Collections.synchronizedMap(LinkedHashMap<String, Bitmap>(10, 1.5f, true))
    private var allocatedSize: Long = 0L
    private var memLimit = 1_000_000L // 1 Mb

    init {
        memLimit = Runtime.getRuntime().maxMemory() / 4
    }

    fun getBitmapById(id: String): Bitmap? {
        return try {
            cache.getOrDefault(id, null)
        } catch (e: NullPointerException) {
            null
        }
    }

    fun putBitmapInCache(id: String, bitmap: Bitmap) {
        if (cache.containsKey(id)) {
            allocatedSize -= getSizeInBytes(cache[id])
        }
        cache[id] = bitmap
        allocatedSize += getSizeInBytes(cache[id])
        checkSize()
    }

    fun clear() {
        cache.clear()
    }

    private fun checkSize() {
        if (allocatedSize > memLimit) {
            val cacheIterator = cache.entries.iterator()
            while (cacheIterator.hasNext()) {
                val entry = cacheIterator.next()
                allocatedSize -= getSizeInBytes(entry.value)
                cacheIterator.remove()
                if (allocatedSize <= memLimit)
                    break
            }
        }
    }

    private fun getSizeInBytes(bitmap: Bitmap?): Long
            = if (bitmap == null) 0L
    else bitmap.rowBytes * bitmap.height.toLong()
}