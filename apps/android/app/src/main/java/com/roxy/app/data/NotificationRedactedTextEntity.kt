package com.roxy.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_redacted_text")
data class NotificationRedactedTextEntity(
    @PrimaryKey val id: String,
    val metadataIdentityDigest: String,
    val packageName: String,
    val occurredAtEpochMillis: Long,
    val title: String?,
    val body: String?,
    val redactionCount: Int,
    val expiresAtEpochMillis: Long,
)
