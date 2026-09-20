package com.example.ui.detail

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CertificateExportHelper
import com.example.crypto.CertificateFormat
import com.example.crypto.KeystoreExportHelper
import com.example.crypto.KeystoreFormatConverter
import com.example.crypto.KeystoreTargetFormat
import com.example.crypto.StorageCompressionHelper
import com.example.data.database.AppDatabase
import com.example.data.model.KeystoreEntity
import com.example.data.repository.KeystoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    // Estados para el paquete completo comprimido All-in-One (.zip)
    private val _isPackagingZip = MutableStateFlow(false)
    val isPackagingZip: StateFlow<Boolean> = _isPackagingZip.asStateFlow()

    private val _zipBundleBytes = MutableStateFlow<ByteArray?>(null)
    val zipBundleBytes: StateFlow<ByteArray?> = _zipBundleBytes.asStateFlow()

    private val _zipBundleError = MutableStateFlow<String?>(null)
    val zipBundleError: StateFlow<String?> = _zipBundleError.asStateFlow()

    // Estadísticas de compresión interna y ahorro de espacio en reposo
    private val _compressionStats = MutableStateFlow<String?>(null)
    val compressionStats: StateFlow<String?> = _compressionStats.asStateFlow()

    // Estados del Conversor de Formatos (JKS ⟷ PKCS12 / .p12)
    private val _isConvertDialogOpen = MutableStateFlow(false)
    val isConvertDialogOpen: StateFlow<Boolean> = _isConvertDialogOpen.asStateFlow()

    private val _targetConvertFormat = MutableStateFlow(KeystoreTargetFormat.PKCS12)
    val targetConvertFormat: StateFlow<KeystoreTargetFormat> = _targetConvertFormat.asStateFlow()

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    private val _convertError = MutableStateFlow<String?>(null)
    val convertError: StateFlow<String?> = _convertError.asStateFlow()

    private val _convertedKeystore = MutableStateFlow<KeystoreEntity?>(null)
    val convertedKeystore: StateFlow<KeystoreEntity?> = _convertedKeystore.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = KeystoreRepository(database.keystoreDao())
    }

    fun loadKeystore(id: Long) {
        viewModelScope.launch {
            repository.getKeystoreById(id).collect { item ->
                _keystore.value = item
                if (item != null) {
                    computeCompressionStats(item)
                }
            }
        }
    }

    /**
     * Calcula métricas reales de compresión del archivo en reposo
     * para verificar el espacio ahorrado mediante compresión ultra-alta.
     */
    private fun computeCompressionStats(keystore: KeystoreEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(keystore.filePath)
                if (file.exists()) {
                    val originalBytes = file.readBytes()
                    val compressed = StorageCompressionHelper.compressWithMaxDeflate(originalBytes)
                    val savings = StorageCompressionHelper.calculateSavingsPercentage(
                        originalSize = originalBytes.size.toLong(),
                        compressedSize = compressed.size.toLong()
                    )
                    _compressionStats.value = "Tamaño en disco: ${originalBytes.size} B | Comprimido: ${compressed.size} B (-${"%.1f".format(savings)}%)"
                }
            } catch (_: Exception) {
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

    /**
     * Obtiene el nombre sugerido para el paquete .zip comprimido al máximo.
     */
    fun getSuggestedZipFileName(): String {
        val current = _keystore.value ?: return "keystore_bundle.zip"
        val safeAlias = current.alias.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return "${safeAlias}_complete_bundle.zip"
    }

    /**
     * Obtiene el nombre sugerido para el archivo Base64.
     */
    fun getSuggestedBase64FileName(): String {
        val current = _keystore.value ?: return "keystore.base64"
        return "${current.fileName}.base64"
    }

    /**
     * Obtiene el nombre sugerido para el archivo Gradle Kotlin DSL.
     */
    fun getSuggestedGradleFileName(): String {
        return "signingConfigs.gradle.kts"
    }

    /**
     * Obtiene el nombre sugerido para el archivo de workflow de GitHub Actions.
     */
    fun getSuggestedWorkflowFileName(): String {
        return "build-and-sign.yml"
    }

    /**
     * Genera el paquete completo ultra-comprimido All-in-One (.zip) en segundo plano.
     */
    fun prepareZipBundle(onReady: ((ByteArray) -> Unit)? = null) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            _isPackagingZip.value = true
            _zipBundleError.value = null
            val result = withContext(Dispatchers.IO) {
                StorageCompressionHelper.buildCompleteZipBundle(current)
            }
            if (result.isSuccess) {
                val bytes = result.getOrThrow()
                _zipBundleBytes.value = bytes
                onReady?.invoke(bytes)
            } else {
                _zipBundleError.value = result.exceptionOrNull()?.localizedMessage ?: "Error al empaquetar archivos"
            }
            _isPackagingZip.value = false
        }
    }

    /**
     * Comparte el paquete .zip completo mediante el FileProvider del sistema.
     */
    fun shareZipBundle(context: Context) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            _isPackagingZip.value = true
            val bytes = _zipBundleBytes.value ?: withContext(Dispatchers.IO) {
                StorageCompressionHelper.buildCompleteZipBundle(current).getOrNull()
            }
            if (bytes != null) {
                _zipBundleBytes.value = bytes
                val fileName = getSuggestedZipFileName()
                val shareResult = StorageCompressionHelper.shareZipFile(context, fileName, bytes)
                if (shareResult.isSuccess) {
                    _securityAlert.value = SecurityAlert(
                        title = "Paquete ZIP Listo",
                        message = "Paquete '$fileName' (${bytes.size} B) preparado para compartir.",
                        isSensitive = false
                    )
                } else {
                    _zipBundleError.value = shareResult.exceptionOrNull()?.localizedMessage
                }
            } else {
                _zipBundleError.value = "No se pudieron empaquetar los archivos para compartir"
            }
            _isPackagingZip.value = false
        }
    }

    /**
     * Guarda el paquete .zip completo en la ruta seleccionada por el usuario mediante SAF (CreateDocument).
     */
    fun saveZipBundleToUri(context: Context, destinationUri: Uri) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            _isPackagingZip.value = true
            val bytes = _zipBundleBytes.value ?: withContext(Dispatchers.IO) {
                StorageCompressionHelper.buildCompleteZipBundle(current).getOrNull()
            }
            if (bytes != null) {
                _zipBundleBytes.value = bytes
                val saveResult = withContext(Dispatchers.IO) {
                    StorageCompressionHelper.writeBytesToUri(context, destinationUri, bytes)
                }
                if (saveResult.isSuccess) {
                    _securityAlert.value = SecurityAlert(
                        title = "Paquete ZIP Guardado",
                        message = "Paquete completo guardado exitosamente (${bytes.size} B con compresión ultra-alta).",
                        isSensitive = false
                    )
                } else {
                    _zipBundleError.value = saveResult.exceptionOrNull()?.localizedMessage
                }
            } else {
                _zipBundleError.value = "No se pudo generar el contenido del paquete comprimido"
            }
            _isPackagingZip.value = false
        }
    }

    /**
     * Guarda el archivo original de la Keystore individualmente en una carpeta seleccionada mediante SAF.
     */
    fun saveKeystoreToUri(context: Context, destinationUri: Uri) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                KeystoreExportHelper.writeKeystoreToUri(context, current, destinationUri)
            }
            if (result.isSuccess) {
                _securityAlert.value = SecurityAlert(
                    title = "Keystore Guardada",
                    message = "Archivo ${current.fileName} guardado individualmente en la carpeta elegida.",
                    isSensitive = false
                )
            } else {
                _securityAlert.value = SecurityAlert(
                    title = "Error al guardar",
                    message = result.exceptionOrNull()?.localizedMessage ?: "No se pudo guardar la keystore",
                    isSensitive = true
                )
            }
        }
    }

    /**
     * Guarda el archivo Base64 (.base64) individualmente en una carpeta seleccionada mediante SAF.
     */
    fun saveBase64ToUri(context: Context, destinationUri: Uri) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            val base64Text = _base64Content.value ?: withContext(Dispatchers.IO) {
                KeystoreExportHelper.generateBase64(current).getOrNull()
            }
            if (base64Text != null) {
                val result = withContext(Dispatchers.IO) {
                    KeystoreExportHelper.writeBase64ToUri(context, base64Text, destinationUri)
                }
                if (result.isSuccess) {
                    _securityAlert.value = SecurityAlert(
                        title = "Base64 Guardado",
                        message = "Archivo ${current.fileName}.base64 guardado exitosamente.",
                        isSensitive = false
                    )
                } else {
                    _securityAlert.value = SecurityAlert(
                        title = "Error al guardar",
                        message = result.exceptionOrNull()?.localizedMessage ?: "No se pudo guardar el archivo Base64",
                        isSensitive = true
                    )
                }
            }
        }
    }

    /**
     * Guarda el archivo Gradle (.gradle.kts) individualmente con SAF.
     */
    fun saveGradleSnippetToUri(context: Context, destinationUri: Uri) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                KeystoreExportHelper.writeGradleSnippetToUri(context, current, destinationUri)
            }
            if (result.isSuccess) {
                _securityAlert.value = SecurityAlert(
                    title = "Configuración Gradle Guardada",
                    message = "Archivo signingConfigs.gradle.kts guardado con éxito.",
                    isSensitive = false
                )
            } else {
                _securityAlert.value = SecurityAlert(
                    title = "Error al guardar",
                    message = result.exceptionOrNull()?.localizedMessage ?: "No se pudo guardar el archivo Gradle",
                    isSensitive = true
                )
            }
        }
    }

    /**
     * Guarda el archivo de workflow de GitHub Actions (.yml) individualmente con SAF.
     */
    fun saveGitHubWorkflowToUri(context: Context, destinationUri: Uri) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                KeystoreExportHelper.writeGitHubWorkflowToUri(context, current, destinationUri)
            }
            if (result.isSuccess) {
                _securityAlert.value = SecurityAlert(
                    title = "Workflow Guardado",
                    message = "Archivo build-and-sign.yml guardado con éxito.",
                    isSensitive = false
                )
            } else {
                _securityAlert.value = SecurityAlert(
                    title = "Error al guardar",
                    message = result.exceptionOrNull()?.localizedMessage ?: "No se pudo guardar el workflow",
                    isSensitive = true
                )
            }
        }
    }

    /**
     * Abre el diálogo modal interactivo de conversión de formato.
     */
    fun openConvertDialog(suggestedFormat: KeystoreTargetFormat? = null) {
        val current = _keystore.value
        val defaultTarget = suggestedFormat ?: if (current?.fileName?.lowercase()?.endsWith(".p12") == true) {
            KeystoreTargetFormat.JKS
        } else {
            KeystoreTargetFormat.PKCS12
        }
        _targetConvertFormat.value = defaultTarget
        _convertError.value = null
        _isConvertDialogOpen.value = true
    }

    /**
     * Cierra el diálogo de conversión de formato.
     */
    fun closeConvertDialog() {
        _isConvertDialogOpen.value = false
        _convertError.value = null
    }

    /**
     * Cambia el formato de destino seleccionado en el diálogo.
     */
    fun setTargetConvertFormat(format: KeystoreTargetFormat) {
        _targetConvertFormat.value = format
    }

    /**
     * Ejecuta la conversión de formato criptográfico en segundo plano y registra
     * la nueva llave resultante en la base de datos Room.
     */
    fun convertFormat(
        context: Context,
        targetFormat: KeystoreTargetFormat,
        storePassword: String,
        keyPassword: String,
        customFileName: String? = null,
        onSuccess: (Long) -> Unit
    ) {
        val current = _keystore.value ?: return
        _isConverting.value = true
        _convertError.value = null

        viewModelScope.launch {
            val result = KeystoreFormatConverter.convertKeystore(
                context = context,
                sourceKeystore = current,
                targetFormat = targetFormat,
                targetStorePassword = storePassword.ifBlank { current.storePassword },
                targetKeyPassword = keyPassword.ifBlank { current.keyPassword },
                customFileName = customFileName
            )

            _isConverting.value = false
            result.onSuccess { newEntity ->
                val newId = repository.insertKeystore(newEntity)
                val saved = newEntity.copy(id = newId)
                _convertedKeystore.value = saved
                _isConvertDialogOpen.value = false
                _securityAlert.value = SecurityAlert(
                    title = "Conversión Exitosa",
                    message = "Llave convertida '${saved.fileName}' guardada en tu lista en formato ${targetFormat.label}.",
                    isSensitive = false
                )
                onSuccess(newId)
            }.onFailure { error ->
                _convertError.value = error.localizedMessage ?: "Error desconocido durante la conversión"
            }
        }
    }

    fun deleteKeystore(onDeleted: () -> Unit) {
        val current = _keystore.value ?: return
        viewModelScope.launch {
            repository.deleteKeystore(current)
            onDeleted()
        }
    }
}

