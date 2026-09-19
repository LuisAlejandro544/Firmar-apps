package com.example.ui.detail

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.KeystoreExportHelper
import com.example.data.database.AppDatabase
import com.example.data.model.KeystoreEntity
import com.example.data.repository.KeystoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Modelo de datos para alertas y avisos in-app de seguridad de la aplicación.
 * Permite advertir al usuario cuando se copian datos sensibles al portapapeles
 * en lugar de usar notificaciones nativas toscas de Android.
 */
data class SecurityAlert(
    val title: String,
    val message: String,
    val isSensitive: Boolean,
    val id: Long = System.currentTimeMillis()
)

/**
 * ViewModel para la pantalla de detalle de una Keystore individual.
 * Maneja visibilidad de contraseñas, generación de Base64 para CI/CD,
 * y copiado seguro con alertas personalizadas de privacidad.
 */
class KeystoreDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KeystoreRepository
    private val _keystore = MutableStateFlow<KeystoreEntity?>(null)
    val keystore: StateFlow<KeystoreEntity?> = _keystore.asStateFlow()

    private val _isStorePasswordRevealed = MutableStateFlow(false)
    val isStorePasswordRevealed: StateFlow<Boolean> = _isStorePasswordRevealed.asStateFlow()

    private val _isKeyPasswordRevealed = MutableStateFlow(false)
    val isKeyPasswordRevealed: StateFlow<Boolean> = _isKeyPasswordRevealed.asStateFlow()

    // Estado para la generación de Base64
    private val _base64Content = MutableStateFlow<String?>(null)
    val base64Content: StateFlow<String?> = _base64Content.asStateFlow()

    private val _isGeneratingBase64 = MutableStateFlow(false)
    val isGeneratingBase64: StateFlow<Boolean> = _isGeneratingBase64.asStateFlow()

    private val _base64Error = MutableStateFlow<String?>(null)
    val base64Error: StateFlow<String?> = _base64Error.asStateFlow()

    // Estado para alertas in-app de seguridad de la app (no toscas de Android)
    private val _securityAlert = MutableStateFlow<SecurityAlert?>(null)
    val securityAlert: StateFlow<SecurityAlert?> = _securityAlert.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = KeystoreRepository(database.keystoreDao())
    }

    fun loadKeystore(id: Long) {
        viewModelScope.launch {
            repository.getKeystoreById(id).collect { item ->
                _keystore.value = item
            }
        }
    }

    fun toggleStorePasswordVisibility() {
        _isStorePasswordRevealed.value = !_isStorePasswordRevealed.value
    }

    fun toggleKeyPasswordVisibility() {
        _isKeyPasswordRevealed.value = !_isKeyPasswordRevealed.value
    }

    /**
     * Genera la cadena codificada en Base64 a partir del archivo .jks físico con 1 clic.
     */
    fun generateBase64() {
        val current = _keystore.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingBase64.value = true
            _base64Error.value = null
            val result = KeystoreExportHelper.generateBase64(current)
            if (result.isSuccess) {
                _base64Content.value = result.getOrNull()
            } else {
                _base64Error.value = result.exceptionOrNull()?.message ?: "Error al convertir a Base64"
            }
            _isGeneratingBase64.value = false
        }
    }

    /**
     * Limpia la vista previa del Base64 generado.
     */
    fun clearBase64() {
        _base64Content.value = null
        _base64Error.value = null
    }

    /**
     * Copia un valor sensible (contraseña o Base64 completo) al portapapeles,
     * suprimiendo la notificación nativa por defecto de Android y disparando
     * la advertencia in-app de seguridad de la app.
     * Se programa el borrado automático del portapapeles a los 2 minutos exclusivamente si aún contiene este dato.
     */
    fun copySensitiveValue(context: Context, label: String, value: String) {
        KeystoreExportHelper.copyToClipboard(context, label, value, isSensitive = true)
        _securityAlert.value = SecurityAlert(
            title = "Copiado: $label (Auto-limpieza en 2 min)",
            message = "Aviso de Seguridad: Otras apps podrían acceder a datos en portapapeles. Se eliminará automáticamente en 2 minutos solo si aún contiene este dato.",
            isSensitive = true
        )
    }

    /**
     * Copia un valor no confidencial (como nombre de archivo o fragmentos de Gradle)
     * mostrando la confirmación elegante in-app de la app.
     */
    fun copyNonSensitiveValue(context: Context, label: String, value: String) {
        KeystoreExportHelper.copyToClipboard(context, label, value, isSensitive = false)
        _securityAlert.value = SecurityAlert(
            title = "Copiado al portapapeles",
            message = "$label copiado. Se limpiará del portapapeles automáticamente en 2 minutos.",
            isSensitive = false
        )
    }

    /**
     * Cierra la notificación in-app de seguridad.
     */
    fun dismissSecurityAlert() {
        _securityAlert.value = null
    }

    fun deleteKeystore(onDeleted: () -> Unit) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            repository.deleteKeystore(current)
            onDeleted()
        }
    }
}

