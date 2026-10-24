package com.example.memorialdays.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EventEntity::class],
    version = 5,
    exportSchema = false
)
abstract class MemorialDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        private const val DB_NAME = "memorial_days.db"

        @Volatile
        private var instance: MemorialDatabase? = null

        fun get(context: Context): MemorialDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MemorialDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
        }

        /**
         * Version 1 shipped without the optional note column. Never use
         * fallbackToDestructiveMigration(): user-entered days must survive upgrades.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN note TEXT")
            }
        }

        /**
         * Version 3 adds recurrence, semantic type, lunar mode and manual order.
         * Defaults are declared in the entity so fresh installs match this SQL.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN isYearly INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN eventType TEXT NOT NULL DEFAULT 'GENERAL'"
                )
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN isLunar INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Version 4 pins an event to the top of the single list. Defaults to
         * unpinned so existing rows keep their manual order.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Version 5 adds a per-card accent colour. 0 means "no tint" and keeps
         * the plain surface colour.
         */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN color INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
