package com.noirmusic.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.noirmusic.ui.theme.SakugaThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "noir_theme_prefs")

interface ThemePreferencesStore {
    val themeMode: Flow<SakugaThemeMode>
    suspend fun setThemeMode(mode: SakugaThemeMode)
}

class DataStoreThemePreferencesStore(context: Context) : ThemePreferencesStore {
    private val dataStore = context.applicationContext.themeDataStore

    override val themeMode: Flow<SakugaThemeMode> = dataStore.data.map { prefs ->
        decodeMode(prefs[KEY_THEME_MODE])
    }

    override suspend fun setThemeMode(mode: SakugaThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("sakuga_theme_mode")

        fun decodeMode(raw: String?): SakugaThemeMode = when (raw) {
            SakugaThemeMode.MIDNIGHT.name -> SakugaThemeMode.MIDNIGHT
            else -> SakugaThemeMode.DAYLIGHT
        }
    }
}
