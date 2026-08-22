package com.roxy.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationSanitizerTest {
    @Test
    fun `redacts synthetic OTP contact and account-like strings`() {
        val result = NotificationSanitizer.sanitize(FutureNotificationContent("Synthetic", "Code 123456 for demo@example.test, call +91 98765 43210"))!!
        assertFalse(result.body!!.contains("123456"))
        assertFalse(result.body!!.contains("demo@example.test"))
        assertFalse(result.body!!.contains("98765"))
        assertEquals(3, result.redactionCount)
    }

    @Test
    fun `drops unsupported sensitive field containers`() {
        assertNull(NotificationSanitizer.sanitize(FutureNotificationContent("Synthetic", "safe", hasActions = true)))
        assertNull(NotificationSanitizer.sanitize(FutureNotificationContent("Synthetic", "safe", hasRemoteView = true)))
    }
}
