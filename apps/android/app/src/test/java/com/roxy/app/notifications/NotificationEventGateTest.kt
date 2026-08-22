package com.roxy.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class NotificationEventGateTest {
    private val allowed = listOf(NotificationPackageRule("example.safe", NotificationPackagePolicy.METADATA_ONLY))

    @Test
    fun `disabled and blocked packages create no metadata event`() {
        assertNull(NotificationEventGate.create(false, allowed, "example.safe", "notification.posted", 10, 11, ZoneId.of("UTC"), "id", "key", false))
        assertNull(NotificationEventGate.create(true, listOf(NotificationPackageRule("example.blocked", NotificationPackagePolicy.BLOCKED)), "example.blocked", "notification.posted", 10, 11, ZoneId.of("UTC"), "id", "key", false))
    }

    @Test
    fun `allowed package event has only metadata fields`() {
        val event = NotificationEventGate.create(true, allowed, "example.safe", "notification.posted", 10, 11, ZoneId.of("Asia/Calcutta"), "id", "key", false)!!
        assertEquals("id", event.id)
        assertEquals("notification.posted", event.eventKind)
        assertEquals("example.safe", event.packageName)
        assertEquals(64, event.identityDigest.length)
    }

    @Test
    fun `group summaries and missing identities fail closed`() {
        assertNull(NotificationEventGate.create(true, allowed, "example.safe", "notification.posted", 10, 11, ZoneId.of("UTC"), "id", "key", true))
        assertNull(NotificationEventGate.create(true, allowed, "example.safe", "notification.posted", 10, 11, ZoneId.of("UTC"), "id", "", false))
    }

    @Test
    fun `same lifecycle identity has a stable digest across updates and removals`() {
        val posted = NotificationEventGate.create(true, allowed, "example.safe", "notification.posted", 10, 11, ZoneId.of("UTC"), "post", "opaque-key", false)!!
        val removed = NotificationEventGate.create(true, allowed, "example.safe", "notification.removed", 10, 12, ZoneId.of("UTC"), "remove", "opaque-key", false)!!
        assertEquals(posted.identityDigest, removed.identityDigest)
    }

    @Test
    fun `lifecycle classifies one object updates removals and callback replays deterministically`() {
        assertEquals(NotificationLifecycle.Transition("notification.posted", 10), NotificationLifecycle.transition("notification.posted", 10, 11, null))
        assertEquals(NotificationLifecycle.Transition("notification.updated", 20), NotificationLifecycle.transition("notification.posted", 10, 20, NotificationLifecycleState("notification.posted", 10)))
        assertEquals(NotificationLifecycle.Transition("notification.updated", 30), NotificationLifecycle.transition("notification.posted", 10, 30, NotificationLifecycleState("notification.updated", 20)))
        assertEquals(NotificationLifecycle.Transition("notification.removed", 31), NotificationLifecycle.transition("notification.removed", 10, 31, NotificationLifecycleState("notification.updated", 30)))
        assertNull(NotificationLifecycle.transition("notification.removed", 10, 31, NotificationLifecycleState("notification.removed", 31)))
        assertNull(NotificationLifecycle.transition("notification.posted", 10, 30, NotificationLifecycleState("notification.updated", 30)))
        assertEquals(NotificationLifecycle.Transition("notification.posted", 40), NotificationLifecycle.transition("notification.posted", 40, 41, NotificationLifecycleState("notification.removed", 31)))
    }
}
