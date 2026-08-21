package com.roxy.app.timeline

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageSummaryReaderTest {
    @Test fun `parses aggregate total apps and incomplete state`() {
        assertEquals(
            UsageSummaryResult.Success(
                totalMillis = 120_000,
                apps = listOf(TodayApp("opaque-app-id", 60_000)),
                reason = "coverage_not_proven",
            ),
            UsageSummaryReader.parse(
                """{"totalDurationMillis":120000,"topApps":[{"appId":"opaque-app-id","durationMillis":60000}],"completeness":{"status":"incomplete","reason":"coverage_not_proven"}}""",
            ),
        )
    }

    @Test fun `rejects malformed or unsupported completeness state`() {
        assertEquals(UsageSummaryResult.Error("summary_invalid_response"), UsageSummaryReader.parse("""{"totalDurationMillis":1000}"""))
        assertEquals(UsageSummaryResult.Error("summary_invalid_response"), UsageSummaryReader.parse("""{"totalDurationMillis":1000,"completeness":{"reason":"complete"}}"""))
    }

    @Test fun `uses a non-identifying local fallback when a label is unavailable`() {
        assertEquals("Unavailable on this phone", UsageSummaryReader.displayLabel(null))
        assertEquals("Example", UsageSummaryReader.displayLabel("Example"))
    }
}
