package com.roxy.app.usage

import com.roxy.app.data.UsageBucketEntity
import com.roxy.app.data.UsageBucketExportKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageBucketExporterTest {
    @Test fun `aggregate payload contains only package identifier and duration`() {
        assertEquals(
            mapOf("packageName" to "example.alpha", "durationMillis" to 1_200L),
            UsageBucketExporter.payloadFields("example.alpha", 1_200),
        )
    }

    @Test fun `active export keys make queueing idempotent`() {
        val bucket = UsageBucketEntity("example.alpha", 0, 1_200)
        val active = setOf(UsageBucketExportKey(bucket.packageName, bucket.bucketStartEpochMillis))

        assertEquals(listOf(bucket), UsageBucketExporter.unqueuedBuckets(listOf(bucket), emptySet()))
        assertTrue(UsageBucketExporter.unqueuedBuckets(listOf(bucket), active).isEmpty())
    }

    @Test fun `only settled buckets can leave the phone`() {
        val settled = UsageBucketEntity("example.alpha", 0, 1_200)
        val stillOpen = UsageBucketEntity("example.beta", UsageAggregation.BUCKET_MILLIS, 600)

        assertEquals(listOf(settled), UsageBucketExporter.settledBuckets(listOf(settled, stillOpen), UsageAggregation.BUCKET_MILLIS * 2))
    }
}
