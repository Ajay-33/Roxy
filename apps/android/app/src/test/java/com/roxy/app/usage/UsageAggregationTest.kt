package com.roxy.app.usage
import org.junit.Assert.assertEquals
import org.junit.Test
class UsageAggregationTest {
 @Test fun `splits a session at fifteen minute boundary`() {
  assertEquals(listOf(UsageBucket("example.alpha", 0, 100), UsageBucket("example.alpha", UsageAggregation.BUCKET_MILLIS, 100)), UsageAggregation.aggregate(listOf(UsageObservation("example.alpha","foreground",UsageAggregation.BUCKET_MILLIS-100),UsageObservation("example.alpha","background",UsageAggregation.BUCKET_MILLIS+100))))
 }
}
