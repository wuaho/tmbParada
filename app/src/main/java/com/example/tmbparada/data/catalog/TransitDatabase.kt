package com.example.tmbparada.data.catalog

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Read-only stop catalog shipped in `assets/tmb_transit.db`.
 *
 * To ship a new catalog, regenerate the asset with `PRAGMA user_version` set to a new
 * value and bump [version] to match: the destructive fallback then makes Room replace the
 * installed copy with the new asset.
 */
@Database(
    entities = [
        BusLineEntity::class,
        StopEntity::class,
        LineStopEntity::class,
        StopSearchFtsEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class TransitDatabase : RoomDatabase() {
    abstract fun transitDao(): TransitDao

    companion object {
        private const val ASSET_NAME = "tmb_transit.db"

        fun create(context: Context): TransitDatabase =
            Room.databaseBuilder(context.applicationContext, TransitDatabase::class.java, ASSET_NAME)
                .createFromAsset(ASSET_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
