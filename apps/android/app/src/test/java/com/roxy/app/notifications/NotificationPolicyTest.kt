package com.roxy.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationPolicyTest {
    @Test
    fun `new package rules are blocked by default`() {
        assertEquals(
            NotificationPackageRule("example.bank", NotificationPackagePolicy.BLOCKED),
            NotificationPolicy.defaultRule(" example.bank "),
        )
    }

    @Test
    fun `owner can explicitly change a valid package to metadata only`() {
        val blocked = NotificationPolicy.defaultRule("example.auth")!!

        assertEquals(
            listOf(NotificationPackageRule("example.auth", NotificationPackagePolicy.METADATA_ONLY)),
            NotificationPolicy.update(listOf(blocked), "example.auth", NotificationPackagePolicy.METADATA_ONLY),
        )
    }

    @Test
    fun `redacted text requires an explicit package policy`() {
        val blocked = NotificationPolicy.defaultRule("example.safe")!!
        val textEnabled = NotificationPolicy.update(listOf(blocked), "example.safe", NotificationPackagePolicy.TEXT_REDACTED)
        assertEquals(NotificationPackagePolicy.TEXT_REDACTED, NotificationPolicy.policyFor(textEnabled, "example.safe"))
        assertEquals(NotificationPackagePolicy.BLOCKED, NotificationPolicy.policyFor(emptyList(), "example.safe"))
    }

    @Test
    fun `invalid package identifiers do not create rules`() {
        assertNull(NotificationPolicy.defaultRule("not a package"))
        assertEquals(emptyList<NotificationPackageRule>(), NotificationPolicy.update(emptyList(), "not a package", NotificationPackagePolicy.BLOCKED))
    }

    @Test
    fun `collector state distinguishes disabled setup needed ready and revoked`() {
        assertEquals(NotificationCollectorStatus.DISABLED, NotificationPolicy.collectorStatus(false, false, false, false))
        assertEquals(NotificationCollectorStatus.LISTENER_NOT_INSTALLED, NotificationPolicy.collectorStatus(true, false, false, false))
        assertEquals(NotificationCollectorStatus.ACCESS_NEEDED, NotificationPolicy.collectorStatus(true, true, false, false))
        assertEquals(NotificationCollectorStatus.READY, NotificationPolicy.collectorStatus(true, true, true, true))
        assertEquals(NotificationCollectorStatus.REVOKED, NotificationPolicy.collectorStatus(true, true, false, true))
    }
}
