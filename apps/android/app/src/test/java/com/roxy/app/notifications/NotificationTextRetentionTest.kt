package com.roxy.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextRetentionTest {
    @Test
    fun `only rows at or beyond seven days expire`() {
        val start = 1_000_000L
        val expiry = NotificationTextRetention.expiresAt(start)
        assertEquals(setOf("expired"), NotificationTextRetention.expiredIds(listOf("fresh" to expiry + 1, "expired" to expiry), expiry))
    }
}
