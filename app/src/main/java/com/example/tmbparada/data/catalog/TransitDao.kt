package com.example.tmbparada.data.catalog

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransitDao {
    /** Stops matching [matchQuery]; an exact match on [exactStopCode] is listed first. */
    @Query(
        """
        SELECT stops.stop_code,
               stops.stop_name,
               GROUP_CONCAT(DISTINCT bus_lines.line_name) AS line_names
        FROM stops
        LEFT JOIN line_stops ON line_stops.stop_code = stops.stop_code
        LEFT JOIN bus_lines ON bus_lines.line_id = line_stops.line_id
        WHERE stops.stop_code IN (
            SELECT CAST(stop_code AS INTEGER)
            FROM stops_fts
            WHERE stops_fts MATCH :matchQuery
        )
        GROUP BY stops.stop_code, stops.stop_name
        ORDER BY stops.stop_code = :exactStopCode DESC, stops.stop_name COLLATE NOCASE
        LIMIT :limit
        """,
    )
    fun searchStops(matchQuery: String, exactStopCode: Int, limit: Int): Flow<List<StopSearchRow>>
}
