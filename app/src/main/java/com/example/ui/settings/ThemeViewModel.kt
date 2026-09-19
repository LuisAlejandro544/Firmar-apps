package com.example.ui.settings

import android.app.Application
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel que proporciona acceso reactivo a las preferencias de tema y color
 * a lo largo de toda la jerarquía de vistas de la aplicación.
 */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = ThemePreferences.getInstance(application)

    /**
     * Flujo de estado con las opciones actuales de tema, modo y color primario.
     */
    val themeSettings: StateFlow<ThemeSettings> = preferences.settingsFlow

    /**
     * Indica si el dispositivo físico o emulador soporta Material You (Android 12 / API 31+).
     */
    val isMaterialYouSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /**
     * Cambia el modo de tema (Sistema, Claro, Oscuro).
     */
    fun setThemeMode(mode: AppThemeMode) {
        preferences.setThemeMode(mode)
    }

    /**
     * Alterna el uso de colores dinámicos de Material You si el sistema lo permite.
     */
    fun setUseDynamicColor(enabled: Boolean) {
        preferences.setUseDynamicColor(enabled)
    }

    /**
     * Establece un color primario personalizado.
     */
    fun setCustomPrimaryColor(color: Color) {
        preferences.setCustomPrimaryColor(color)
    }

    /**
     * Restablece el color primario al índigo por defecto de la app.
     */
    fun resetToDefaultColor() {
        preferences.setCustomPrimaryColor(Color(0xFF3B5DF6))
    }
}
