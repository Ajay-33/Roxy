package com.roxy.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_health")
data class SyncHealthEntity(
    @PrimaryKey val id: Int = 1,
    val lastAttemptEpochMillis: Long? = null,
    val lastSuccessEpochMillis: Long? = null,
    val lastErrorCode: String? = null,
)
