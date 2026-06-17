package com.attentionmirror.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UsageRecord::class, AdMark::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun adMarkDao(): AdMarkDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attention_mirror.db",
                )
                    // Pre-release: usage is re-derived from the system each run,
                    // so a destructive upgrade is acceptable for now.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
