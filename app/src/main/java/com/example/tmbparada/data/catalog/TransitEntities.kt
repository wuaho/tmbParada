package com.example.tmbparada.data.catalog

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

// These entities must match the schema of the packaged asset `tmb_transit.db`:
// Room validates its identity hash when the database is opened.

@Entity(tableName = "bus_lines")
data class BusLineEntity(
    @PrimaryKey
    @ColumnInfo(name = "line_id")
    val lineId: String,
    @ColumnInfo(name = "line_name")
    val lineName: String,
    @ColumnInfo(name = "header_color")
    val headerColor: String,
)

@Entity(tableName = "stops")
data class StopEntity(
    @PrimaryKey
    @ColumnInfo(name = "stop_code")
    val stopCode: Int,
    @ColumnInfo(name = "stop_name")
    val stopName: String,
    val latitude: Double?,
    val longitude: Double?,
)

@Entity(
    tableName = "line_stops",
    primaryKeys = ["line_id", "stop_code"],
    indices = [Index(value = ["stop_code"])],
)
data class LineStopEntity(
    @ColumnInfo(name = "line_id")
    val lineId: String,
    @ColumnInfo(name = "stop_code")
    val stopCode: Int,
)

/** Accent-free search index: one row per stop and line, see [toFtsPrefixQuery]. */
@Fts4
@Entity(tableName = "stops_fts")
data class StopSearchFtsEntity(
    @ColumnInfo(name = "stop_code")
    val stopCode: String,
    @ColumnInfo(name = "stop_name")
    val stopName: String,
    @ColumnInfo(name = "line_name")
    val lineName: String,
)

data class StopSearchRow(
    @ColumnInfo(name = "stop_code")
    val stopCode: Int,
    @ColumnInfo(name = "stop_name")
    val stopName: String,
    @ColumnInfo(name = "line_names")
    val lineNames: String?,
)
