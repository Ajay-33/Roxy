package com.roxy.app.notifications
import org.junit.Assert.assertEquals
import org.junit.Test
class NotificationAnalyticsReaderTest {
 @Test fun `parses deterministic metadata analytics`() { val result=NotificationAnalyticsReader.parse("""{"count":3,"postedCount":1,"updatedCount":1,"removedCount":1,"period":"week","hourly":[{"hour":9,"count":2}],"topApps":[{"packageName":"example.safe","count":3}],"bursts":[{"startHour":9,"count":3}],"completeness":{"status":"incomplete","reason":"coverage_not_proven"}}"""); assertEquals(NotificationAnalyticsResult.Success(NotificationAnalytics(3,1,1,1,"week",listOf(NotificationHour(9,2)),listOf(NotificationSource("example.safe",3)),listOf(NotificationBurst(9,3)),NotificationCompleteness("incomplete","coverage_not_proven"))),result) }
 @Test fun `keeps reading older analytics responses without updates`() { val result=NotificationAnalyticsReader.parse("""{"count":2,"postedCount":1,"removedCount":1,"hourly":[],"topApps":[]}"""); assertEquals(NotificationAnalyticsResult.Success(NotificationAnalytics(2,1,0,1,"day",emptyList(),emptyList(),emptyList(),NotificationCompleteness("incomplete","coverage_not_proven"))),result) }
 @Test fun `rejects incomplete analytics`() { assertEquals(NotificationAnalyticsResult.Error("notification_analytics_invalid_response"), NotificationAnalyticsReader.parse("""{"count":3}""")) }
}
