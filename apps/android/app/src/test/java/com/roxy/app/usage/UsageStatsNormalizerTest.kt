package com.roxy.app.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageStatsNormalizerTest {
    @Test fun `removes invalid samples and combines duplicate packages`() {
        assertEquals(
            listOf(UsageSample("example.alpha", 900), UsageSample("example.beta", 100)),
            UsageStatsNormalizer.normalize(listOf(
                UsageSample("example.alpha", 400), UsageSample("", 20), UsageSample("example.alpha", 500),
                UsageSample("example.beta", 100), UsageSample("example.ignored", 0),
            )),
        )
    }

    @Test fun `saturates duplicate durations instead of overflowing`() {
        assertEquals(
            listOf(UsageSample("example.alpha", Long.MAX_VALUE)),
            UsageStatsNormalizer.normalize(listOf(UsageSample("example.alpha", Long.MAX_VALUE), UsageSample("example.alpha", 1))),
        )
    }
}
