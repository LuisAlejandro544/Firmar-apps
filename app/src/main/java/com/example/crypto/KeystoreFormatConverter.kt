package com.example.crypto

import android.content.Context
import com.example.data.model.KeystoreEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate

/**
 * Formatos de destino soportados para la conversión de almacenes de claves.
 */
enum class KeystoreTargetFormat(
    val label: String,
    val extension: String,
    val description: String,
    val storeType: String
) {
    PKCS12(
        label = "PKCS#12 (.p12)",
        extension = ".p12",
        description = "Estándar moderno universal según RFC 7292. Compatible con Android Studio, apksigner, iOS, servidores y navegadores.",
        storeType = "PKCS12"
    ),
    JKS(
        label = "Java KeyStore (.jks)",
        extension = ".jks",
        description = "Formato propietario tradicional de Java y Android Studio para empaquetar firmas de producción.",
        storeType = "JKS"
    ),
    KEYSTORE(
        label = "Keystore Clásico (.keystore)",
        extension = ".keystore",
        description = "Contenedor clásico habitualmente empleado en plantillas de Flutter, React Native, Unity y Gradle.",
        storeType = "PKCS12"
    )
}

/**
 * Motor de conversión criptográfica bidireccional entre formatos JKS, PKCS12 (.p12) y Keystore clásico.
 * 
 * Permite migrar llaves existentes entre estándares sin perder la validez del certificado X.509,
 * las huellas digitales (SHA-1 / SHA-256) ni la clave privada de firma.
 */
object KeystoreFormatConverter {

    private val bcProvider: BouncyCastleProvider by lazy {
        BouncyCastleProvider()
    }

    /**
     * Detecta el formato aproximado de una Keystore según su extensión de archivo o cabecera.
     */
    fun detectCurrentFormat(fileName: String): KeystoreTargetFormat {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".p12") -> KeystoreTargetFormat.PKCS12
            lower.endsWith(".keystore") -> KeystoreTargetFormat.KEYSTORE
            else -> KeystoreTargetFormat.JKS
        }
    }

    /**
     * Convierte una Keystore existente a otro formato de almacén de claves (.jks ⟷ .p12 / PKCS12),
     * generando un nuevo archivo físico y retornando la entidad lista para persistir en Room.
     *
     * @param context Contexto de Android para acceder a archivos internos.
     * @param sourceKeystore Entidad de la keystore original.
     * @param targetFormat Formato de destino (PKCS12, JKS o KEYSTORE).
     * @param targetStorePassword Nueva contraseña para el almacén (o la misma si se desea mantener).
     * @param targetKeyPassword Nueva contraseña para la clave (o la misma si se desea mantener).
     * @param customFileName Nombre personalizado opcional para el nuevo archivo.
     * @return Result con la nueva KeystoreEntity generada.
     */
    suspend fun convertKeystore(
        context: Context,
        sourceKeystore: KeystoreEntity,
        targetFormat: KeystoreTargetFormat,
        targetStorePassword: String,
        targetKeyPassword: String,
        customFileName: String? = null
    ): Result<KeystoreEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(sourceKeystore.filePath)
            if (!sourceFile.exists()) {
                throw IllegalStateException("El archivo original no existe en: ${sourceKeystore.filePath}")
            }

            // 1. Cargar el almacén de claves de origen probando con múltiples proveedores
            val sourceStore = loadSourceKeyStore(sourceFile, sourceKeystore.storePassword)

            val alias = sourceKeystore.alias.trim()
            if (!sourceStore.containsAlias(alias)) {
                // Si el alias guardado no coincide exactamente, buscar el primer alias disponible
                val aliases = sourceStore.aliases().toList()
                if (aliases.isEmpty()) {
                    throw IllegalStateException("El almacén original no contiene ninguna clave privada o certificado.")
                }
            }

            val effectiveAlias = if (sourceStore.containsAlias(alias)) alias else sourceStore.aliases().nextElement()

            // 2. Extraer clave privada y cadena de certificados
            val keyPasswordChars = sourceKeystore.keyPassword.toCharArray()
            val privateKey = runCatching {
                sourceStore.getKey(effectiveAlias, keyPasswordChars) as? PrivateKey
            }.getOrNull() ?: runCatching {
                // Fallback con la contraseña del almacén
                sourceStore.getKey(effectiveAlias, sourceKeystore.storePassword.toCharArray()) as? PrivateKey
            }.getOrNull() ?: throw IllegalStateException("No se pudo descifrar la clave privada con las contraseñas provistas.")

            val certChain = sourceStore.getCertificateChain(effectiveAlias)
                ?: arrayOf<Certificate>(sourceStore.getCertificate(effectiveAlias)
                    ?: throw IllegalStateException("No se encontró ningún certificado X.509 asociado al alias '$effectiveAlias'."))

            // 3. Crear el nuevo KeyStore en el formato destino
            val targetStore = createTargetKeyStore(targetFormat)
            targetStore.load(null, null) // Inicializar vacío

            // 4. Asignar la entrada de clave y certificados
            targetStore.setKeyEntry(
                effectiveAlias,
                privateKey,
                targetKeyPassword.toCharArray(),
                certChain
            )

            // 5. Determinar el nombre final del archivo generado
            val sanitizedBase = if (!customFileName.isNullOrBlank()) {
                customFileName.trim()
                    .removeSuffix(".jks")
                    .removeSuffix(".keystore")
                    .removeSuffix(".p12")
                    .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            } else {
                val originalBase = sourceKeystore.fileName
                    .removeSuffix(".jks")
                    .removeSuffix(".keystore")
                    .removeSuffix(".p12")
                val formatSuffix = when (targetFormat) {
                    KeystoreTargetFormat.PKCS12 -> "converted_p12"
                    KeystoreTargetFormat.JKS -> "converted_jks"
                    KeystoreTargetFormat.KEYSTORE -> "converted_keystore"
                }
                "${originalBase}_$formatSuffix"
            }
            val finalFileName = "$sanitizedBase${targetFormat.extension}"

            // 6. Preparar directorio de destino y escribir el nuevo archivo
            val keystoresDir = File(context.filesDir, "keystores").apply { mkdirs() }
            val destinationFile = File(keystoresDir, finalFileName)

            FileOutputStream(destinationFile).use { fos ->
                targetStore.store(fos, targetStorePassword.toCharArray())
            }

            // 7. Calcular huellas digitales del certificado principal
            val mainCert = certChain.firstOrNull() as? X509Certificate
            val sha256Fingerprint = mainCert?.let { calculateFingerprint(it.encoded, "SHA-256") }
                ?: sourceKeystore.sha256Fingerprint
            val sha1Fingerprint = mainCert?.let { calculateFingerprint(it.encoded, "SHA-1") }
                ?: sourceKeystore.sha1Fingerprint

            // 8. Construir la nueva entidad para Room
            KeystoreEntity(
                title = "${sourceKeystore.title} (${targetFormat.label.substringBefore(" ")})",
                fileName = finalFileName,
                filePath = destinationFile.absolutePath,
                fileSizeBytes = destinationFile.length(),
                alias = effectiveAlias,
                storePassword = targetStorePassword,
                keyPassword = targetKeyPassword,
                keyAlgorithm = sourceKeystore.keyAlgorithm,
                validityYears = sourceKeystore.validityYears,
                commonName = sourceKeystore.commonName,
                organization = sourceKeystore.organization,
                organizationalUnit = sourceKeystore.organizationalUnit,
                city = sourceKeystore.city,
                state = sourceKeystore.state,
                countryCode = sourceKeystore.countryCode,
                sha256Fingerprint = sha256Fingerprint,
                sha1Fingerprint = sha1Fingerprint,
                createdAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * Intenta cargar el archivo de origen buscando el tipo de KeyStore compatible (PKCS12, JKS, BKS).
     */
    private fun loadSourceKeyStore(file: File, password: String): KeyStore {
        val storeTypes = listOf("PKCS12", "JKS", "BKS")
        var lastException: Throwable? = null

        for (type in storeTypes) {
            val keyStore = runCatching {
                if (type == "JKS" || type == "BKS") {
                    KeyStore.getInstance(type, bcProvider)
                } else {
                    KeyStore.getInstance(type)
                }
            }.getOrNull() ?: continue

            try {
                FileInputStream(file).use { fis ->
                    keyStore.load(fis, password.toCharArray())
                }
                return keyStore
            } catch (e: Throwable) {
                lastException = e
            }
        }

        throw IllegalStateException(
            "No se pudo abrir el almacén de claves de origen. Verifica que la contraseña sea correcta. (${lastException?.localizedMessage})",
            lastException
        )
    }

    /**
     * Crea una instancia de KeyStore para el formato de destino seleccionado.
     */
    private fun createTargetKeyStore(targetFormat: KeystoreTargetFormat): KeyStore {
        return when (targetFormat) {
            KeystoreTargetFormat.PKCS12, KeystoreTargetFormat.KEYSTORE -> {
                KeyStore.getInstance("PKCS12")
            }
            KeystoreTargetFormat.JKS -> {
                runCatching {
                    KeyStore.getInstance("JKS", bcProvider)
                }.recoverCatching {
                    KeyStore.getInstance("PKCS12")
                }.getOrThrow()
            }
        }
    }

    /**
     * Calcula la huella digital en formato hexadecimal separado por dos puntos (AA:BB:CC:...)
     */
    private fun calculateFingerprint(data: ByteArray, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm).digest(data)
        return digest.joinToString(":") { byte ->
            String.format("%02X", byte)
        }
    }
}
