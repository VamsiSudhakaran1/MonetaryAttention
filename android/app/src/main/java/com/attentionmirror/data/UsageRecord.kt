package com.attentionmirror.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per (package, local day) of foreground usage. Mirrors the backend's
 * `usage_events` table. We store seconds, not screen content.
 */
@Entity(
    tableName = "usage_records",
    primaryKeys = ["packageName", "localDate"],
)
data class UsageRecord(
    val packageName: String,
    /** ISO local date, e.g. "2026-06-17". */
    val localDate: String,
    val appName: String,
    val durationSeconds: Long,
)

/** Projection used by the per-platform comparison query. */
data class PackageSeconds(
    @androidx.room.ColumnInfo(name = "packageName") val packageName: String,
    @androidx.room.ColumnInfo(name = "totalSeconds") val totalSeconds: Long,
)

/** Projection used by the weekly report (seconds per package per day). */
data class DatePackageSeconds(
    @androidx.room.ColumnInfo(name = "localDate") val localDate: String,
    @androidx.room.ColumnInfo(name = "packageName") val packageName: String,
    @androidx.room.ColumnInfo(name = "totalSeconds") val totalSeconds: Long,
)
