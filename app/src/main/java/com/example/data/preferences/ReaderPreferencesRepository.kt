package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ReaderSurfaceMode
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerDataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_prefs")

data class ReaderPreferences(
    val fontSizeSp: Float = 18f,
    val lineHeightMultiplier: Float = 1.65f,
    val useSerifFont: Boolean = true,
    val readerSurfaceMode: ReaderSurfaceMode = ReaderSurfaceMode.FOLLOW_APP,
    val appThemeMode: AppThemeMode = AppThemeMode.DARK,
    val ambientMusicEnabled: Boolean = true,
    val ambientMusicVolume: Float = 0.55f
)

class ReaderPreferencesRepository(private val context: Context) {

    private object Keys {
        val FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
        val LINE_HEIGHT_MULTIPLIER = floatPreferencesKey("line_height_multiplier")
        val USE_SERIF_FONT = booleanPreferencesKey("use_serif_font")
        val READER_SURFACE = stringPreferencesKey("reader_surface")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val AMBIENT_MUSIC_ENABLED = booleanPreferencesKey("ambient_music_enabled")
        val AMBIENT_MUSIC_VOLUME = floatPreferencesKey("ambient_music_volume")
    }

    val preferencesFlow: Flow<ReaderPreferences> = context.readerDataStore.data.map { prefs ->
        val surfaceName = prefs[Keys.READER_SURFACE] ?: ReaderSurfaceMode.FOLLOW_APP.name
        val themeName = prefs[Keys.APP_THEME_MODE] ?: AppThemeMode.DARK.name
        ReaderPreferences(
            fontSizeSp = (prefs[Keys.FONT_SIZE_SP] ?: 18f).coerceIn(14f, 26f),
            lineHeightMultiplier = (prefs[Keys.LINE_HEIGHT_MULTIPLIER] ?: 1.65f).coerceIn(1.35f, 2.0f),
            useSerifFont = prefs[Keys.USE_SERIF_FONT] ?: true,
            readerSurfaceMode = runCatching { ReaderSurfaceMode.valueOf(surfaceName) }
                .getOrDefault(ReaderSurfaceMode.FOLLOW_APP),
            appThemeMode = runCatching { AppThemeMode.valueOf(themeName) }
                .getOrDefault(AppThemeMode.DARK),
            ambientMusicEnabled = prefs[Keys.AMBIENT_MUSIC_ENABLED] ?: true,
            ambientMusicVolume = (prefs[Keys.AMBIENT_MUSIC_VOLUME] ?: 0.55f).coerceIn(0.05f, 1f)
        )
    }

    suspend fun setFontSize(sizeSp: Float) {
        context.readerDataStore.edit { prefs ->
            prefs[Keys.FONT_SIZE_SP] = sizeSp.coerceIn(14f, 26f)
        }
    }

    suspend fun setLineHeightMultiplier(multiplier: Float) {
        context.readerDataStore.edit { prefs ->
            prefs[Keys.LINE_HEIGHT_MULTIPLIER] = multiplier.coerceIn(1.35f, 2.0f)
        }
    }

    suspend fun setUseSerifFont(useSerif: Boolean) {
        context.readerDataStore.edit { prefs ->
            prefs[Keys.USE_SERIF_FONT] = useSerif
        }
    }

    suspend fun setReaderSurfaceMode(mode: ReaderSurfaceMode) {
        context.readerDataStore.edit { prefs ->
            prefs[Keys.READER_SURFACE] = mode.name
        }
    }

    suspend fun setAppThemeMode(mode: AppThemeMode) {
        context.readerDataStore.edit { prefs ->
            prefs[Keys.APP_THEME_MODE] = mode.name
        }
    }

    suspend fun setAmbientMusicEnabled(enabled: Boolean) {
        context.readerDataStore.edit { prefs ->
            prefs[Keys.AMBIENT_MUSIC_ENABLED] = enabled
        }
    }

    suspend fun setAmbientMusicVolume(volume: Float) {
        context.readerDataStore.edit { prefs ->
            prefs[Keys.AMBIENT_MUSIC_VOLUME] = volume.coerceIn(0.05f, 1f)
        }
    }
}
