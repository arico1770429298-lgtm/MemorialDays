package com.example.memorialdays.ui

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A tiny one-shot throttle. It is deliberately not a debounce: the first action
 * always wins, duplicate taps inside 300ms are ignored. The mutex makes the
 * check atomic for concurrent coroutines.
 */
class ActionThrottle(private val intervalMs: Long = 300L) {
    private val mutex = Mutex()
    private var lastAtNanos: Long = 0L

    suspend fun allow(): Boolean = mutex.withLock {
        val now = System.nanoTime()
        val elapsedMs = (now - lastAtNanos) / 1_000_000L
        if (lastAtNanos != 0L && elapsedMs < intervalMs) {
            false
        } else {
            lastAtNanos = now
            true
        }
    }
}
