package com.attentionmirror.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsageDao {

    /** Upsert: re-reporting a day overwrites that day's value (latest wins). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<UsageRecord>)

    @Query(
        "SELECT packageName, SUM(durationSeconds) AS totalSeconds " +
            "FROM usage_records WHERE localDate = :day GROUP BY packageName"
    )
    suspend fun secondsForDay(day: String): List<PackageSeconds>

    @Query(
        "SELECT packageName, SUM(durationSeconds) AS totalSeconds " +
            "FROM usage_records WHERE localDate BETWEEN :start AND :end " +
            "GROUP BY packageName"
    )
    suspend fun secondsBetween(start: String, end: String): List<PackageSeconds>
}
