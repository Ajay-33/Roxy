package com.roxy.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(entities = [LocalEventEntity::class, SyncHealthEntity::class, UsageCollectionCursor::class, UsageObservationEntity::class, UsageBucketEntity::class, UsageBucketExportEntity::class, NotificationMetadataEntity::class, NotificationRedactedTextEntity::class], version = 9, exportSchema = true)
abstract class RoxyDatabase : RoomDatabase() {
    abstract fun localEventDao(): LocalEventDao
    abstract fun syncHealthDao(): SyncHealthDao
    abstract fun usageCollectionDao(): UsageCollectionDao
    abstract fun notificationMetadataDao(): NotificationMetadataDao
    abstract fun notificationRedactedTextDao(): NotificationRedactedTextDao

    companion object {
        fun create(context: Context): RoxyDatabase = Room.databaseBuilder(
            context.applicationContext,
            RoxyDatabase::class.java,
            "roxy.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS sync_health (id INTEGER NOT NULL, lastAttemptEpochMillis INTEGER, lastSuccessEpochMillis INTEGER, lastErrorCode TEXT, PRIMARY KEY(id))")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS usage_collection_cursor (id INTEGER NOT NULL, collectedUntilEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))")
                database.execSQL("CREATE TABLE IF NOT EXISTS usage_observations (id TEXT NOT NULL, packageName TEXT NOT NULL, eventType TEXT NOT NULL, occurredAtEpochMillis INTEGER NOT NULL, recordedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) { override fun migrate(database: SupportSQLiteDatabase) { database.execSQL("CREATE TABLE IF NOT EXISTS usage_15m_buckets (packageName TEXT NOT NULL, bucketStartEpochMillis INTEGER NOT NULL, durationMillis INTEGER NOT NULL, PRIMARY KEY(packageName, bucketStartEpochMillis))") } }
        private val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(database: SupportSQLiteDatabase) { database.execSQL("CREATE TABLE IF NOT EXISTS usage_bucket_exports (packageName TEXT NOT NULL, bucketStartEpochMillis INTEGER NOT NULL, eventId TEXT NOT NULL, PRIMARY KEY(packageName, bucketStartEpochMillis))") } }
        private val MIGRATION_5_6 = object : Migration(5, 6) { override fun migrate(database: SupportSQLiteDatabase) { database.execSQL("CREATE TABLE IF NOT EXISTS notification_metadata_events (id TEXT NOT NULL, eventKind TEXT NOT NULL, occurredAtEpochMillis INTEGER NOT NULL, recordedAtEpochMillis INTEGER NOT NULL, observedTimezone TEXT NOT NULL, packageName TEXT NOT NULL, source TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))") } }
        private val MIGRATION_6_7 = object : Migration(6, 7) { override fun migrate(database: SupportSQLiteDatabase) { database.execSQL("ALTER TABLE notification_metadata_events ADD COLUMN identityDigest TEXT"); database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notification_metadata_events_identityDigest ON notification_metadata_events(identityDigest)") } }
        private val MIGRATION_7_8 = object : Migration(7, 8) { override fun migrate(database: SupportSQLiteDatabase) { database.execSQL("CREATE TABLE IF NOT EXISTS notification_redacted_text (id TEXT NOT NULL, metadataIdentityDigest TEXT NOT NULL, packageName TEXT NOT NULL, occurredAtEpochMillis INTEGER NOT NULL, title TEXT, body TEXT, redactionCount INTEGER NOT NULL, expiresAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(id))") } }
        private val MIGRATION_8_9 = object : Migration(8, 9) { override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DROP INDEX IF EXISTS index_notification_metadata_events_identityDigest")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notification_metadata_events_identityDigest_eventKind_occurredAtEpochMillis ON notification_metadata_events(identityDigest, eventKind, occurredAtEpochMillis)")
        } }
    }
}
