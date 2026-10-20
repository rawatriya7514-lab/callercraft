package com.craftcaller.app.util

import com.craftcaller.app.data.CallerInfo
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Cross-component hub shared by the UI, the ringing service and the system
 * call-screening component (all in the same process).
 */
object CallerHub {
    /** Bumped whenever some component changes blocklist/spam data. */
    val refreshTick = MutableStateFlow(0)

    fun forceRefresh() {
        refreshTick.value = refreshTick.value + 1
    }

    /** Ring time cache: last incoming number captured by the receivers. */
    @Volatile var lastRingingNumber: String = ""

    @Volatile var callInProgress: Boolean = false

    /** Lru-ish cache for resolved values during a ring. */
    private val cache = HashMap<String, CallerInfo>()

    @Synchronized
    fun put(n: String, info: CallerInfo) {
        if (cache.size > 200) cache.clear()
        cache[n] = info
    }

    @Synchronized
    fun get(n: String): CallerInfo? = cache[n]

    @Synchronized
    fun clearCaches() {
        cache.clear()
    }
}
