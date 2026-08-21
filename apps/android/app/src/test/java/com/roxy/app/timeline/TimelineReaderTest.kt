package com.roxy.app.timeline

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineReaderTest {
    @Test fun `parses only count and completeness`() {
        assertEquals(TimelineReadResult.Success(2, "coverage_not_proven"), TimelineReader.parse("""{"items":[{},{}],"completeness":{"status":"incomplete","reason":"coverage_not_proven"}}"""))
    }
    @Test fun `rejects malformed completeness`() {
        assertEquals(TimelineReadResult.Error("timeline_invalid_response"), TimelineReader.parse("""{"items":[]}"""))
    }
}
