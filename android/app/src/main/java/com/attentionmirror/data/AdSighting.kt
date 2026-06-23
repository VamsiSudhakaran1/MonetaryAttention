package com.attentionmirror.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * One detected ad impression from the opt-in ad scanner (`full` flavor): which
 * app, when it appeared, when it left the screen (→ how long it was shown), and
 * which marker matched. We store the marker keyword only ("sponsored"), never
 * any other screen content. Used for richer reporting (count, on-screen time,
 * frequency); the manual `ad_marks` count still drives calibration.
 */
@Entity(tableName = "ad_sightings")
data class AdSighting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    /** ISO local date, e.g. "2026-06-17". */
    val localDate: String,
    val startMillis: Long,
    val endMillis: Long,
    val marker: String,
)

/** Per-package rollup of ad sightings for a day. */
data class AdStat(
    @ColumnInfo(name = "packageName") val packageName: String,
    @ColumnInfo(name = "count") val count: Int,
    @ColumnInfo(name = "totalSeconds") val totalSeconds: Long,
)

@Dao
interface AdSightingDao {

    @Insert
    suspend fun insert(sighting: AdSighting)

    @Query(
        "SELECT packageName, COUNT(*) AS count, " +
            "SUM((endMillis - startMillis) / 1000) AS totalSeconds " +
            "FROM ad_sightings WHERE localDate = :day GROUP BY packageName"
    )
    suspend fun statsForDay(day: String): List<AdStat>
}
