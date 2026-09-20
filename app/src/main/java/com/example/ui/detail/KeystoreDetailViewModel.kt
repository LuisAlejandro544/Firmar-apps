package com.example.ui.detail

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CertificateExportHelper
import com.example.crypto.CertificateFormat
import com.example.crypto.KeystoreExportHelper
import com.example.data.database.AppDatabase
import com.example.data.model.KeystoreEntity
import com.example.data.repository.KeystoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Estado para la exportación de certificados públicos (.pem / .crt / .der)
    private val _certificatePem = MutableStateFlow<String?>(null)
    val certificatePem: StateFlow<String?> = _certificatePem.asStateFlow()

    private val _isExtractingCert = MutableStateFlow(false)
    val isExtractingCert: StateFlow<Boolean> = _isExtractingCert.asStateFlow()

    private val _certError = MutableStateFlow<String?>(null)
    val certError: StateFlow<String?> = _certError.asStateFlow()

    private val _selectedCertFormat = MutableStateFlow(CertificateFormat.PEM)
    val selectedCertFormat: StateFlow<CertificateFormat> = _selectedCertFormat.asStateFlow()

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

    /**
     * Cambia el formato de certificado seleccionado (PEM, CRT o DER).
     */
    fun setCertificateFormat(format: CertificateFormat) {
        _selectedCertFormat.value = format
    }

    /**
     * Extrae el certificado X.509 del almacén y genera su representación en formato PEM (texto).
     */
    fun loadCertificatePem() {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            _isExtractingCert.value = true
            _certError.value = null
            withContext(Dispatchers.IO) {
                val certResult = CertificateExportHelper.extractCertificate(current)
                if (certResult.isSuccess) {
                    val cert = certResult.getOrThrow()
                    val pemText = CertificateExportHelper.formatAsPem(cert)
                    _certificatePem.value = pemText
                } else {
                    _certError.value = certResult.exceptionOrNull()?.localizedMessage
                        ?: "No se pudo extraer el certificado público"
                }
            }
            _isExtractingCert.value = false
        }
    }

    /**
     * Limpia la vista previa del PEM del certificado.
     */
    fun clearCertificatePem() {
        _certificatePem.value = null
        _certError.value = null
    }

    /**
     * Copia el texto PEM del certificado al portapapeles.
     * Al tratarse de un certificado público (sin claves privadas), no representa un riesgo crítico,
     * pero se notifica amigablemente al usuario con la confirmación in-app.
     */
    fun copyCertificatePem(context: Context) {
        val pem = _certificatePem.value ?: return
        val current = _keystore.value ?: return
        KeystoreExportHelper.copyToClipboard(context, "Certificado PEM (${current.alias})", pem, isSensitive = false)
        _securityAlert.value = SecurityAlert(
            title = "Certificado PEM copiado",
            message = "El certificado público X.509 para '${current.alias}' ha sido copiado al portapapeles. Es seguro para compartir.",
            isSensitive = false
        )
    }

    /**
     * Comparte el certificado público en el formato especificado (.pem, .crt o .der)
     * mediante el FileProvider del sistema.
     */
    fun shareCertificate(context: Context, format: CertificateFormat) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            _isExtractingCert.value = true
            val result = withContext(Dispatchers.IO) {
                CertificateExportHelper.shareCertificate(context, current, format)
            }
            if (result.isFailure) {
                _certError.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Error al compartir el certificado ${format.label}"
            } else {
                _securityAlert.value = SecurityAlert(
                    title = "Certificado ${format.label} listo",
                    message = "Archivo ${CertificateExportHelper.getSuggestedFileName(current, format)} preparado para compartir.",
                    isSensitive = false
                )
            }
            _isExtractingCert.value = false
        }
    }

    /**
     * Guarda el certificado público en el destino seleccionado por el usuario mediante SAF (CreateDocument).
     */
    fun saveCertificateToUri(context: Context, destinationUri: Uri, format: CertificateFormat) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            _isExtractingCert.value = true
            val result = withContext(Dispatchers.IO) {
                val certResult = CertificateExportHelper.extractCertificate(current)
                if (certResult.isFailure) {
                    return@withContext Result.failure(certResult.exceptionOrNull() ?: Exception("Certificado no disponible"))
                }
                val cert = certResult.getOrThrow()
                val data = CertificateExportHelper.exportCertificateData(cert, format)
                CertificateExportHelper.writeCertificateToUri(context, destinationUri, data)
            }
            if (result.isSuccess) {
                _securityAlert.value = SecurityAlert(
                    title = "Certificado Guardado",
                    message = "El archivo ${format.label} se guardó exitosamente en la ubicación seleccionada.",
                    isSensitive = false
                )
            } else {
                _certError.value = result.exceptionOrNull()?.localizedMessage ?: "Error al guardar el archivo de certificado"
            }
            _isExtractingCert.value = false
        }
    }

    /**
     * Obtiene el nombre sugerido para el archivo del certificado según el formato.
     */
    fun getSuggestedFileName(format: CertificateFormat): String {
        val current = _keystore.value ?: return "cert.${format.extension}"
        return CertificateExportHelper.getSuggestedFileName(current, format)
    }

    fun deleteKeystore(onDeleted: () -> Unit) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            repository.deleteKeystore(current)
            onDeleted()
        }
    }
}

