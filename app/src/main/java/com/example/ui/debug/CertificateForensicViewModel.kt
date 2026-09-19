package com.example.ui.debug

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.KeystoreGenerator
import com.example.crypto.KeystoreParams
import com.example.data.database.AppDatabase
import com.example.data.model.KeystoreEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Modelo con los resultados de la auditoría forense del certificado X.509 y la firma real.
 */
data class ForensicInspectionResult(
    val keystoreTitle: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val alias: String,
    val keyAlgorithm: String,
    val keySizeBits: Int,
    val publicExponent: String,
    val serialNumberHex: String,
    val serialNumberDec: String,
    val signatureAlgorithm: String,
    val signatureAlgorithmOid: String,
    val validFromFormatted: String,
    val validToFormatted: String,
    val isCurrentlyValid: Boolean,
    val validityRemainingText: String,
    val sha256Fingerprint: String,
    val sha1Fingerprint: String,
    val md5Fingerprint: String,
    val subjectDn: String,
    val issuerDn: String,
    val rdnFields: Map<String, String>,
    // Verificación matemática de firma real
    val testPayload: String,
    val signatureSampleHex: String,
    val signatureSizeBytes: Int,
    val isSignatureValid: Boolean,
    val signatureVerificationStatus: String
)

sealed interface ForensicUiState {
    data object Idle : ForensicUiState
    data object Loading : ForensicUiState
    data class Success(val result: ForensicInspectionResult) : ForensicUiState
    data class Error(val message: String) : ForensicUiState
}

/**
 * ViewModel encargado de realizar la inspección forense byte a byte del archivo Keystore,
 * analizando la estructura ASN.1 del certificado X.509 y ejecutando una prueba de firma
 * matemática real con la clave privada y pública para descartar cualquier simulación.
 */
class CertificateForensicViewModel(application: Application) : AndroidViewModel(application) {

    private val keystoreDao = AppDatabase.getDatabase(application).keystoreDao()

    private val _availableKeystores = MutableStateFlow<List<KeystoreEntity>>(emptyList())
    val availableKeystores: StateFlow<List<KeystoreEntity>> = _availableKeystores.asStateFlow()

    private val _uiState = MutableStateFlow<ForensicUiState>(ForensicUiState.Idle)
    val uiState: StateFlow<ForensicUiState> = _uiState.asStateFlow()

    init {
        loadAvailableKeystores()
    }

    fun loadAvailableKeystores() {
        viewModelScope.launch {
            keystoreDao.getAllKeystores().collect { list ->
                _availableKeystores.value = list
                // Si hay keystores y estamos en idle, inspeccionar la primera automáticamente
                if (list.isNotEmpty() && _uiState.value is ForensicUiState.Idle) {
                    inspectKeystore(list.first())
                }
            }
        }
    }

    /**
     * Inspecciona una keystore existente registrada en la base de datos de la app.
     */
    fun inspectKeystore(entity: KeystoreEntity) {
        viewModelScope.launch {
            _uiState.value = ForensicUiState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    performForensicAudit(
                        title = entity.title,
                        fileName = entity.fileName,
                        filePath = entity.filePath,
                        alias = entity.alias,
                        storePassword = entity.storePassword,
                        keyPassword = entity.keyPassword
                    )
                }
                _uiState.value = ForensicUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = ForensicUiState.Error("Error al inspeccionar: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Genera una clave efímera temporal para realizar la inspección forense si el usuario
     * no tiene aún ninguna keystore creada en la app principal.
     */
    fun inspectEphemeralTestKeystore(keySizeBits: Int = 2048) {
        viewModelScope.launch {
            _uiState.value = ForensicUiState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val testDir = File(context.cacheDir, "debug_forensic")
                    if (!testDir.exists()) testDir.mkdirs()
                    val testFile = File(testDir, "forensic_test.jks")

                    // Crear almacén de prueba con el generador real
                    val params = KeystoreParams(
                        title = "Keystore de Auditoría en Vivo",
                        fileName = "forensic_test.jks",
                        alias = "debug_audit_key",
                        storePassword = "AuditStorePassword123!",
                        keyPassword = "AuditKeyPassword123!",
                        validityYears = 1,
                        validityDays = 365,
                        keySize = keySizeBits,
                        commonName = "Inspector Forense Android",
                        organization = "Crypto Lab Debugger",
                        organizationalUnit = "Security Audit Team",
                        countryCode = "MX"
                    )

                    val generatedEntity = KeystoreGenerator.generateKeystore(context, params).getOrThrow()

                    performForensicAudit(
                        title = generatedEntity.title,
                        fileName = generatedEntity.fileName,
                        filePath = generatedEntity.filePath,
                        alias = generatedEntity.alias,
                        storePassword = generatedEntity.storePassword,
                        keyPassword = generatedEntity.keyPassword
                    )
                }
                _uiState.value = ForensicUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = ForensicUiState.Error("Error en prueba en vivo: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Realiza el análisis matemático y criptográfico real y extrae las propiedades ASN.1.
     */
    private fun performForensicAudit(
        title: String,
        fileName: String,
        filePath: String,
        alias: String,
        storePassword: String,
        keyPassword: String
    ): ForensicInspectionResult {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalStateException("El archivo físico no existe en la ruta: $filePath")
        }

        val fileSizeBytes = file.length()

        // 1. Cargar el almacén PKCS12 directamente con el proveedor de seguridad nativo
        val keyStore = KeyStore.getInstance("PKCS12")
        FileInputStream(file).use { fis ->
            keyStore.load(fis, storePassword.toCharArray())
        }

        val cert = keyStore.getCertificate(alias) as? X509Certificate
            ?: throw IllegalStateException("No se encontró el certificado X.509 para el alias '$alias'")

        val privateKey = keyStore.getKey(alias, keyPassword.toCharArray()) as? PrivateKey
            ?: throw IllegalStateException("No se pudo obtener la clave privada con la contraseña proporcionada")

        // 2. Extraer propiedades de la clave pública RSA
        val rsaPublicKey = cert.publicKey as? RSAPublicKey
        val keySizeBits = rsaPublicKey?.modulus?.bitLength() ?: 0
        val exponentBigInt = rsaPublicKey?.publicExponent ?: BigInteger.ZERO
        val exponentHex = "0x" + exponentBigInt.toString(16).uppercase(Locale.ROOT)
        val exponentFormatted = "$exponentBigInt ($exponentHex)"

        // 3. Estructura ASN.1 del Certificado X.509
        val serialNumber = cert.serialNumber
        val serialNumberHex = "0x" + serialNumber.toString(16).uppercase(Locale.ROOT)
        val serialNumberDec = serialNumber.toString(10)
        val sigAlgName = cert.sigAlgName
        val sigAlgOid = cert.sigAlgOID

        // Validez temporal
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss 'UTC'", Locale.getDefault())
        val validFromStr = dateFormat.format(cert.notBefore)
        val validToStr = dateFormat.format(cert.notAfter)

        var isCurrentlyValid = false
        var validityRemainingText = ""
        try {
            cert.checkValidity(Date())
            isCurrentlyValid = true
            val diffMillis = cert.notAfter.time - System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
            validityRemainingText = "Vigente (~$days días restantes)"
        } catch (_: Exception) {
            isCurrentlyValid = false
            validityRemainingText = "Certificado fuera de vigencia temporal"
        }

        // 4. Cálculo de Huellas Criptográficas (SHA-256, SHA-1, MD5)
        val certEncoded = cert.encoded
        val sha256 = formatFingerprint(MessageDigest.getInstance("SHA-256").digest(certEncoded))
        val sha1 = formatFingerprint(MessageDigest.getInstance("SHA-1").digest(certEncoded))
        val md5 = formatFingerprint(MessageDigest.getInstance("MD5").digest(certEncoded))

        // 5. Análisis del Distinguished Name (DN)
        val subjectPrincipal = cert.subjectX500Principal.name
        val issuerPrincipal = cert.issuerX500Principal.name
        val rdnMap = parseRdnFields(subjectPrincipal)

        // 6. PRUEBA DE FIRMA MATEMÁTICA ASIMÉTRICA REAL (Descartar Simulación)
        val testPayload = "KeystoreCreator_AuditPayload_${System.currentTimeMillis()}"
        val payloadBytes = testPayload.toByteArray(Charsets.UTF_8)

        // Firmar con la clave privada
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey)
        signer.update(payloadBytes)
        val signatureBytes = signer.sign()

        // Verificar con la clave pública del certificado
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(cert.publicKey)
        verifier.update(payloadBytes)
        val isSignatureValid = verifier.verify(signatureBytes)

        val signatureSampleHex = signatureBytes.take(32).joinToString(" ") { "%02X".format(it) } + " ... (${signatureBytes.size} bytes totales)"

        val verificationStatus = if (isSignatureValid) {
            "AUTÉNTICA Y COMPROBADA (Par de Claves 100% Funcional)"
        } else {
            "FALLO CRIPTOGRÁFICO: La firma no coincide con el certificado"
        }

        return ForensicInspectionResult(
            keystoreTitle = title,
            fileName = fileName,
            filePath = filePath,
            fileSizeBytes = fileSizeBytes,
            alias = alias,
            keyAlgorithm = cert.publicKey.algorithm,
            keySizeBits = keySizeBits,
            publicExponent = exponentFormatted,
            serialNumberHex = serialNumberHex,
            serialNumberDec = serialNumberDec,
            signatureAlgorithm = sigAlgName,
            signatureAlgorithmOid = sigAlgOid,
            validFromFormatted = validFromStr,
            validToFormatted = validToStr,
            isCurrentlyValid = isCurrentlyValid,
            validityRemainingText = validityRemainingText,
            sha256Fingerprint = sha256,
            sha1Fingerprint = sha1,
            md5Fingerprint = md5,
            subjectDn = subjectPrincipal,
            issuerDn = issuerPrincipal,
            rdnFields = rdnMap,
            testPayload = testPayload,
            signatureSampleHex = signatureSampleHex,
            signatureSizeBytes = signatureBytes.size,
            isSignatureValid = isSignatureValid,
            signatureVerificationStatus = verificationStatus
        )
    }

    private fun formatFingerprint(bytes: ByteArray): String {
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    private fun parseRdnFields(dn: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val parts = dn.split(",")
        for (part in parts) {
            val keyValue = part.trim().split("=", limit = 2)
            if (keyValue.size == 2) {
                map[keyValue[0].trim()] = keyValue[1].trim()
            }
        }
        return map
    }
}
