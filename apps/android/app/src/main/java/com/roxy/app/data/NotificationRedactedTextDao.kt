package com.roxy.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationRedactedTextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(row: NotificationRedactedTextEntity)
    @Query("DELETE FROM notification_redacted_text WHERE expiresAtEpochMillis <= :now") fun deleteExpired(now: Long): Int
}
