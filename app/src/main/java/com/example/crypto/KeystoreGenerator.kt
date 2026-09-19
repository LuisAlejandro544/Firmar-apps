package com.example.crypto

import android.content.Context
import com.example.data.model.KeystoreEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.util.Calendar
import java.util.Date

/**
 * Parámetros requeridos para la generación de un nuevo Keystore y certificado autofirmado.
 * Permite especificar la validez tanto en años como en días exactos (desde 1 día hasta 100 años).
 */
data class KeystoreParams(
    val title: String,
    val fileName: String,
    val alias: String,
    val storePassword: String,
    val keyPassword: String,
    val keySize: Int = 2048,
    val validityYears: Int = 25,
    val validityDays: Int = validityYears * 365,
    val commonName: String = "Android Developer",
    val organization: String = "Mobile Development",
    val organizationalUnit: String = "Development",
    val countryCode: String = "ES"
)

/**
 * Motor criptográfico para la creación de archivos Keystore estándar (formato PKCS12/JKS compatible).
 * Utiliza Bouncy Castle para construir certificados X.509 versión 3 autofirmados válidos para la
 * firma de aplicaciones Android (APK y AAB) mediante apksigner o Gradle.
 */
object KeystoreGenerator {

    private val bcProvider: BouncyCastleProvider by lazy {
        BouncyCastleProvider()
    }

    /**
     * Genera un par de claves RSA, un certificado X.509 autofirmado y empaqueta todo en un
     * almacén Keystore guardado en el directorio interno de la aplicación.
     *
     * @param context Contexto de la aplicación para acceder a filesDir.
     * @param params Parámetros de configuración y credenciales del Keystore.
     * @return KeystoreEntity con la información guardada y las huellas digitales calculadas.
     */
    suspend fun generateKeystore(context: Context, params: KeystoreParams): Result<KeystoreEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Validar y normalizar el nombre del archivo
                val sanitizedBaseName = params.fileName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val finalFileName = if (sanitizedBaseName.endsWith(".jks", ignoreCase = true) ||
                    sanitizedBaseName.endsWith(".keystore", ignoreCase = true)
                ) {
                    sanitizedBaseName
                } else {
                    "$sanitizedBaseName.jks"
                }

                // 2. Preparar el directorio de almacenamiento interno
                val keystoresDir = File(context.filesDir, "keystores")
                if (!keystoresDir.exists()) {
                    keystoresDir.mkdirs()
                }
                val destinationFile = File(keystoresDir, finalFileName)

                // 3. Generar par de claves RSA (2048 o 4096 bits)
                val secureRandom = SecureRandom()
                val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
                keyPairGenerator.initialize(params.keySize, secureRandom)
                val keyPair = keyPairGenerator.generateKeyPair()

                // 4. Construir la identidad del sujeto X.500 (Subject / Issuer)
                val nameBuilder = X500NameBuilder(BCStyle.INSTANCE)
                if (params.commonName.isNotBlank()) {
                    nameBuilder.addRDN(BCStyle.CN, params.commonName.trim())
                }
                if (params.organizationalUnit.isNotBlank()) {
                    nameBuilder.addRDN(BCStyle.OU, params.organizationalUnit.trim())
                }
                if (params.organization.isNotBlank()) {
                    nameBuilder.addRDN(BCStyle.O, params.organization.trim())
                }
                if (params.countryCode.isNotBlank()) {
                    nameBuilder.addRDN(BCStyle.C, params.countryCode.trim().take(2).uppercase())
                }
                val issuerAndSubject = nameBuilder.build()

                // 5. Definir rango de validez (inicio ayer para evitar desincronización de reloj, fin según días exactos)
                val notBefore = Date(System.currentTimeMillis() - (24L * 60 * 60 * 1000))
                val calendar = Calendar.getInstance()
                val totalDays = if (params.validityDays > 0) params.validityDays else (params.validityYears * 365)
                calendar.add(Calendar.DAY_OF_YEAR, totalDays)
                val notAfter = calendar.time

                // Número de serie aleatorio de 64 bits
                val serialNumber = BigInteger(64, secureRandom)

                // 6. Construir y firmar el certificado X.509 v3 autofirmado con SHA256withRSA
                val certBuilder = JcaX509v3CertificateBuilder(
                    issuerAndSubject,
                    serialNumber,
                    notBefore,
                    notAfter,
                    issuerAndSubject,
                    keyPair.public
                )

                // Usar el motor de firma del sistema (Conscrypt/OpenSSL nativo en Android)
                // con fallback a la instancia de Bouncy Castle en memoria
                val contentSigner = runCatching {
                    JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
                }.recoverCatching {
                    JcaContentSignerBuilder("SHA256withRSA")
                        .setProvider(bcProvider)
                        .build(keyPair.private)
                }.getOrThrow()

                val certHolder = certBuilder.build(contentSigner)

                val x509Certificate = runCatching {
                    JcaX509CertificateConverter().getCertificate(certHolder)
                }.recoverCatching {
                    JcaX509CertificateConverter()
                        .setProvider(bcProvider)
                        .getCertificate(certHolder)
                }.getOrThrow()

                // 7. Empaquetar clave privada y certificado en un KeyStore PKCS12 (estándar de la industria)
                val keyStore = KeyStore.getInstance("PKCS12")
                keyStore.load(null, null)

                val certificateChain = arrayOf<Certificate>(x509Certificate)
                keyStore.setKeyEntry(
                    params.alias.trim(),
                    keyPair.private,
                    params.keyPassword.toCharArray(),
                    certificateChain
                )

                // 8. Escribir el archivo físico en el almacenamiento
                FileOutputStream(destinationFile).use { outputStream ->
                    keyStore.store(outputStream, params.storePassword.toCharArray())
                }

                // 9. Calcular huellas digitales SHA-1 y SHA-256 del certificado
                val encodedCert = x509Certificate.encoded
                val sha256Fingerprint = calculateFingerprint(encodedCert, "SHA-256")
                val sha1Fingerprint = calculateFingerprint(encodedCert, "SHA-1")

                // 10. Retornar la entidad resultante
                KeystoreEntity(
                    title = params.title.trim().ifEmpty { "Keystore ${params.alias}" },
                    fileName = finalFileName,
                    filePath = destinationFile.absolutePath,
                    fileSizeBytes = destinationFile.length(),
                    alias = params.alias.trim(),
                    storePassword = params.storePassword,
                    keyPassword = params.keyPassword,
                    keyAlgorithm = "RSA ${params.keySize} bits",
                    validityYears = if (totalDays >= 365) totalDays / 365 else 1,
                    commonName = params.commonName.trim(),
                    organization = params.organization.trim(),
                    organizationalUnit = params.organizationalUnit.trim(),
                    countryCode = params.countryCode.trim().take(2).uppercase(),
                    sha256Fingerprint = sha256Fingerprint,
                    sha1Fingerprint = sha1Fingerprint,
                    createdAt = System.currentTimeMillis()
                )
            }
        }

    /**
     * Calcula la huella digital en formato hexadecimal separado por dos puntos (ej: AA:BB:CC:...)
     */
    private fun calculateFingerprint(data: ByteArray, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm).digest(data)
        return digest.joinToString(":") { byte ->
            String.format("%02X", byte)
        }
    }
}
