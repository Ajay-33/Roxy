package com.roxy.app.notifications
import org.junit.Assert.assertEquals
import org.junit.Test
class NotificationAnalyticsReaderTest {
 @Test fun `parses deterministic metadata analytics`() { assertEquals(NotificationAnalyticsResult.Success(NotificationAnalytics(3,2,1,9,"example.safe",3)), NotificationAnalyticsReader.parse("""{"count":3,"postedCount":2,"removedCount":1,"hourly":[{"hour":9,"count":2}],"topApps":[{"packageName":"example.safe","count":3}]}""")) }
 @Test fun `rejects incomplete analytics`() { assertEquals(NotificationAnalyticsResult.Error("notification_analytics_invalid_response"), NotificationAnalyticsReader.parse("""{"count":3}""")) }
}
