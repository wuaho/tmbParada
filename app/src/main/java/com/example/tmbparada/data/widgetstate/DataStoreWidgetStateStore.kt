package com.example.tmbparada.data.widgetstate

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tmbparada.domain.WidgetStateStore
import com.example.tmbparada.domain.model.WidgetState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Stores each widget as one JSON entry so updates of a widget are atomic. */
class DataStoreWidgetStateStore(
    private val dataStore: DataStore<Preferences>,
) : WidgetStateStore {

    override fun observe(widgetId: Int): Flow<WidgetState> =
        dataStore.data
            .map { preferences -> WidgetStateCodec.decode(preferences[keyFor(widgetId)]) }
            .distinctUntilChanged()

    override fun observeAll(): Flow<Map<Int, WidgetState>> =
        dataStore.data
            .map { preferences ->
                preferences.asMap().entries.mapNotNull { (key, value) ->
                    val widgetId = key.name.removePrefix(KEY_PREFIX).toIntOrNull()
                    if (widgetId == null || !key.name.startsWith(KEY_PREFIX)) return@mapNotNull null
                    widgetId to WidgetStateCodec.decode(value as? String)
                }.toMap()
            }
            .distinctUntilChanged()

    override suspend fun get(widgetId: Int): WidgetState = observe(widgetId).first()

    override suspend fun update(widgetId: Int, transform: (WidgetState) -> WidgetState): WidgetState {
        lateinit var updated: WidgetState
        dataStore.edit { preferences ->
            val key = keyFor(widgetId)
            updated = transform(WidgetStateCodec.decode(preferences[key]))
            preferences[key] = WidgetStateCodec.encode(updated)
        }
        return updated
    }

    override suspend fun delete(widgetId: Int) {
        dataStore.edit { it.remove(keyFor(widgetId)) }
    }

    companion object {
        private const val KEY_PREFIX = "widget_"

        internal fun keyFor(widgetId: Int) = stringPreferencesKey("$KEY_PREFIX$widgetId")
    }
}

/** Single process-wide DataStore; it also imports the SharedPreferences used by version 1.0. */
val Context.widgetStateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_state",
    produceMigrations = { context -> listOf(legacyStateMigration(context)) },
)

private const val LEGACY_PREFERENCES_NAME = "tmb_widget_state"
private const val LEGACY_KEY_PREFIX = "state_"

private fun legacyStateMigration(context: Context) =
    SharedPreferencesMigration<Preferences>(
        context = context,
        sharedPreferencesName = LEGACY_PREFERENCES_NAME,
        migrate = { legacy, current ->
            val migrated = current.toMutablePreferences()
            legacy.getAll().forEach { (key, value) ->
                val widgetId = key.removePrefix(LEGACY_KEY_PREFIX).toIntOrNull()
                if (key.startsWith(LEGACY_KEY_PREFIX) && widgetId != null && value is String) {
                    val state = WidgetStateCodec.migrateLegacy(value)
                    migrated[DataStoreWidgetStateStore.keyFor(widgetId)] = WidgetStateCodec.encode(state)
                }
            }
            migrated.toPreferences()
        },
    )
