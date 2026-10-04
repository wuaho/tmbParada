package com.example.tmbparada.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

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

data class StopSearchResult(
    @ColumnInfo(name = "stop_code")
    val stopCode: Int,
    @ColumnInfo(name = "stop_name")
    val stopName: String,
    @ColumnInfo(name = "line_names")
    val lineNames: String,
    val latitude: Double?,
    val longitude: Double?,
)

@Dao
interface TmbTransitDao {
    @Query(
        """
        SELECT stops.stop_code,
               stops.stop_name,
               GROUP_CONCAT(DISTINCT bus_lines.line_name) AS line_names,
               stops.latitude,
               stops.longitude
        FROM stops
        LEFT JOIN line_stops ON line_stops.stop_code = stops.stop_code
        LEFT JOIN bus_lines ON bus_lines.line_id = line_stops.line_id
        WHERE stops.stop_code IN (
            SELECT CAST(stop_code AS INTEGER)
            FROM stops_fts
            WHERE stops_fts MATCH :matchQuery
        )
        GROUP BY stops.stop_code, stops.stop_name, stops.latitude, stops.longitude
        ORDER BY stops.stop_name COLLATE NOCASE
        LIMIT 30
        """,
    )
    fun searchStops(matchQuery: String): Flow<List<StopSearchResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<StopEntity>)
}

@Database(
    entities = [
        BusLineEntity::class,
        StopEntity::class,
        LineStopEntity::class,
        StopSearchFtsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TmbTransitDatabase : RoomDatabase() {
    abstract fun transitDao(): TmbTransitDao

    companion object {
        private const val DATABASE_NAME = "tmb_transit.db"

        @Volatile
        private var instance: TmbTransitDatabase? = null

        fun getInstance(context: Context): TmbTransitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TmbTransitDatabase::class.java,
                    DATABASE_NAME,
                )
                    .createFromAsset(DATABASE_NAME)
                    .build()
                    .also { instance = it }
            }
    }
}

class TmbTransitSearchRepository(context: Context) {
    private val dao = TmbTransitDatabase.getInstance(context).transitDao()

    fun search(query: String): Flow<List<StopSearchResult>> =
        dao.searchStops(query.toFtsPrefixQuery())
}

private fun String.toFtsPrefixQuery(): String =
    trim()
        .split(WHITESPACE_REGEX)
        .filter(String::isNotBlank)
        .joinToString(" ") { token ->
            token.filter { it.isLetterOrDigit() }.lowercase() + "*"
        }
        .ifBlank { NO_MATCH_QUERY }

private val WHITESPACE_REGEX = Regex("\\s+")
private const val NO_MATCH_QUERY = "__no_match__*"
