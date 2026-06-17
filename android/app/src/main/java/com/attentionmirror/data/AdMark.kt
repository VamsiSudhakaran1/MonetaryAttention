package com.attentionmirror.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

/**
 * Running count of ads the user has marked for a package ("I saw an ad").
 * Combined with tracked time this yields a personal ads/minute rate (see
 * [com.attentionmirror.domain.Calibration]). We store a count, never content.
 */
@Entity(tableName = "ad_marks")
data class AdMark(
    @PrimaryKey val packageName: String,
    val count: Int,
)

@Dao
interface AdMarkDao {

    /** Increment (or create) the mark count for a package by one. */
    @Transaction
    suspend fun increment(packageName: String) {
        insertZero(packageName)
        bump(packageName)
    }

    @Query("INSERT OR IGNORE INTO ad_marks(packageName, count) VALUES(:pkg, 0)")
    suspend fun insertZero(pkg: String)

    @Query("UPDATE ad_marks SET count = count + 1 WHERE packageName = :pkg")
    suspend fun bump(pkg: String)

    @Query("SELECT packageName, count FROM ad_marks")
    suspend fun all(): List<AdMark>
}
