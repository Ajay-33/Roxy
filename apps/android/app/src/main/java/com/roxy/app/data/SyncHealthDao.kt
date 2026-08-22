package com.roxy.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncHealthDao {
    @Query("SELECT * FROM sync_health WHERE id = 1") fun read(): SyncHealthEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun save(health: SyncHealthEntity)
}
