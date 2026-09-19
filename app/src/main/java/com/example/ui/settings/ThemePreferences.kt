package com.example.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Modos de apariencia del tema en la aplicación.
 */
enum class AppThemeMode(val title: String, val description: String) {
    SYSTEM("Seguir el sistema", "Se adapta automáticamente al tema de tu dispositivo Android"),
    LIGHT("Modo claro", "Fondo luminoso de alta legibilidad"),
    DARK("Modo oscuro", "Fondo oscuro de ahorro de batería y descanso visual")
}

/**
 * Estado inmutable de la configuración de tema y colores de la aplicación.
 */
data class ThemeSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val customPrimaryColor: Color = Color(0xFF3B5DF6) // Índigo corporativo predeterminado
)

/**
 * Gestor de preferencias de tema persistido mediante SharedPreferences de Android.
 * Proporciona un StateFlow reactivo para que toda la interfaz se actualice de inmediato.
 */
class ThemePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<ThemeSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): ThemeSettings {
        val modeName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        val themeMode = try {
            AppThemeMode.valueOf(modeName)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
        val useDynamic = prefs.getBoolean(KEY_USE_DYNAMIC_COLOR, false)
        val colorArgb = prefs.getInt(KEY_PRIMARY_COLOR_ARGB, Color(0xFF3B5DF6).toArgb())

        return ThemeSettings(
            themeMode = themeMode,
            useDynamicColor = useDynamic,
            customPrimaryColor = Color(colorArgb)
        )
    }

    /**
     * Actualiza el modo del tema (Sistema, Claro u Oscuro).
     */
    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(themeMode = mode)
    }

    /**
     * Activa o desactiva los colores dinámicos de Material You (Android 12+).
     */
    fun setUseDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_DYNAMIC_COLOR, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(useDynamicColor = enabled)
    }

    /**
     * Guarda el color primario personalizado elegido por el usuario.
     */
    fun setCustomPrimaryColor(color: Color) {
        prefs.edit().putInt(KEY_PRIMARY_COLOR_ARGB, color.toArgb()).apply()
        _settingsFlow.value = _settingsFlow.value.copy(customPrimaryColor = color)
    }

    companion object {
        private const val PREFS_NAME = "keystore_theme_preferences"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_USE_DYNAMIC_COLOR = "key_use_dynamic_color"
        private const val KEY_PRIMARY_COLOR_ARGB = "key_primary_color_argb"

        @Volatile
        private var INSTANCE: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
