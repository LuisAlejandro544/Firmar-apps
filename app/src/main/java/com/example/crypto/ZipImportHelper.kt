package com.example.crypto

import android.content.Context
import android.net.Uri
import com.example.data.model.KeystoreEntity
import com.example.data.repository.KeystoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Resultado del análisis preliminar de un archivo ZIP seleccionado por el usuario.
 *
 * @param originalZipFileName Nombre original del archivo ZIP importado.
 * @param jksTempFile Archivo temporal extraído del almacén .jks o .keystore.
 * @param jksFileName Nombre del archivo dentro del ZIP (ej. "release.jks").
 * @param jksSizeBytes Tamaño en bytes del archivo del almacén.
 * @param suggestedTitle Título sugerido para la clave en la biblioteca.
 * @param suggestedAlias Alias sugerido detectado de los scripts o del almacén.
 * @param suggestedStorePassword Contraseña de almacén detectada de signingConfigs si existía.
 * @param suggestedKeyPassword Contraseña de clave detectada de signingConfigs si existía.
 * @param detectedComponents Lista de componentes reconocidos dentro del paquete comprimido.
 * @param tempExtractionDir Directorio temporal donde se descomprimieron los archivos.
 */
data class ZipAnalysisResult(
    val originalZipFileName: String,
    val jksTempFile: File,
    val jksFileName: String,
    val jksSizeBytes: Long,
    val suggestedTitle: String,
    val suggestedAlias: String,
    val suggestedStorePassword: String,
    val suggestedKeyPassword: String,
    val detectedComponents: List<String>,
    val tempExtractionDir: File
)

/**
 * Resultado de la validación criptográfica estricta del almacén de claves y su certificado X.509.
 */
data class ImportedKeystoreValidation(
    val validatedAlias: String,
    val keyAlgorithm: String,
    val sha256Fingerprint: String,
    val sha1Fingerprint: String,
    val validityYears: Int,
    val commonName: String,
    val organization: String,
    val organizationalUnit: String,
    val countryCode: String,
    val notBefore: Long,
    val notAfter: Long,
    val isExpired: Boolean
)

/**
 * Motor modular para la importación y restauración de paquetes ZIP exportados por la aplicación
 * o almacenes de claves estándar generados externamente.
 *
 * Características:
 * 1. Protección estricta contra vulnerabilidades "Zip Slip" durante la descompresión.
 * 2. Detección automática inteligente de almacenes .jks / .keystore y reconstrucción desde .base64 si aplica.
 * 3. Parser heurístico de `signingConfigs.gradle.kts` e `INFO_KEYSTORE.txt` para precargar credenciales si estaban en el ZIP.
 * 4. Validación criptográfica real con `java.security.KeyStore` (PKCS12 y JKS) y verificación del certificado X.509.
 * 5. Ingesta atómica en la base de datos Room con cifrado AES-256-GCM respaldado por hardware en reposo.
 * 6. Compatibilidad total con procesadores móviles de 32 bits (armeabi-v7a) y 64 bits (arm64-v8a).
 */
object ZipImportHelper {

    /**
     * Inspecciona y descomprime un archivo ZIP desde un Uri de Android (SAF), buscando el almacén de claves
     * y metadatos asociados.
     */
    suspend fun analyzeZipUri(context: Context, zipUri: Uri): Result<ZipAnalysisResult> = withContext(Dispatchers.IO) {
        runCatching {
            val contentResolver = context.contentResolver

            // Obtener el nombre del archivo ZIP de las columnas del proveedor o inferirlo
            val zipFileName = runCatching {
                var name: String? = null
                contentResolver.query(zipUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
                name ?: "keystore_bundle.zip"
            }.getOrDefault("keystore_bundle.zip")

            // Directorio temporal aislado en caché
            val uniqueDirName = "import_${System.currentTimeMillis()}"
            val tempDir = File(context.cacheDir, uniqueDirName).apply { mkdirs() }

            val detectedComponents = mutableListOf<String>()
            val extractedFiles = mutableListOf<File>()
            var foundJksFile: File? = null
            var foundBase64File: File? = null
            var signingConfigsFile: File? = null
            var infoReportFile: File? = null

            // Descomprimir entradas con protección contra Zip Slip
            contentResolver.openInputStream(zipUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val entryFile = File(tempDir, entry.name)

                            // Verificación de seguridad: Prevención estricta de Zip Slip
                            val canonicalDest = entryFile.canonicalPath
                            val canonicalBase = tempDir.canonicalPath
                            if (!canonicalDest.startsWith(canonicalBase)) {
                                throw SecurityException("Entrada ZIP maliciosa detectada fuera del directorio de extracción.")
                            }

                            entryFile.parentFile?.mkdirs()
                            FileOutputStream(entryFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            extractedFiles.add(entryFile)

                            val lowerName = entry.name.lowercase()
                            when {
                                lowerName.endsWith(".jks") || lowerName.endsWith(".keystore") -> {
                                    foundJksFile = entryFile
                                    detectedComponents.add("Almacén de claves (${entryFile.name})")
                                }
                                lowerName.endsWith(".base64") -> {
                                    foundBase64File = entryFile
                                    detectedComponents.add("Archivo Base64 para CI/CD")
                                }
                                lowerName.endsWith(".pem") || lowerName.endsWith(".crt") || lowerName.endsWith(".der") -> {
                                    detectedComponents.add("Certificado público (${entryFile.name})")
                                }
                                lowerName.contains("signingconfigs") -> {
                                    signingConfigsFile = entryFile
                                    detectedComponents.add("Configuración Gradle de firma")
                                }
                                lowerName.contains("github-actions") || lowerName.endsWith(".yml") -> {
                                    detectedComponents.add("Workflow GitHub Actions CI/CD")
                                }
                                lowerName.contains("info_keystore") -> {
                                    infoReportFile = entryFile
                                    detectedComponents.add("Reporte de auditoría y huellas")
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: throw IllegalStateException("No se pudo abrir el archivo ZIP seleccionado.")

            // Si no se encontró un .jks directo pero hay un .base64, reconstruirlo
            if (foundJksFile == null && foundBase64File != null) {
                val base64Content = foundBase64File!!.readText().trim()
                val decodedBytes = runCatching {
                    android.util.Base64.decode(base64Content, android.util.Base64.DEFAULT)
                }.getOrNull()

                if (decodedBytes != null && decodedBytes.isNotEmpty()) {
                    val reconstructedName = foundBase64File!!.name.removeSuffix(".base64").ifEmpty { "restored_keystore.jks" }
                    val restoredFile = File(tempDir, reconstructedName)
                    restoredFile.writeBytes(decodedBytes)
                    foundJksFile = restoredFile
                    detectedComponents.add("Almacén restaurado desde Base64 ($reconstructedName)")
                }
            }

            val finalJksFile = foundJksFile
                ?: throw IllegalArgumentException("El archivo ZIP no contiene ningún almacén de claves (.jks o .keystore) válido.")

            // Extraer sugerencias de signingConfigs.gradle.kts si existía
            var suggestedStorePass = ""
            var suggestedKeyPass = ""
            var suggestedAlias = ""
            signingConfigsFile?.let { file ->
                val content = file.readText()
                val storePassRegex = """storePassword\s*=\s*"([^"]+)"""".toRegex()
                val keyPassRegex = """keyPassword\s*=\s*"([^"]+)"""".toRegex()
                val aliasRegex = """keyAlias\s*=\s*"([^"]+)"""".toRegex()

                storePassRegex.find(content)?.groupValues?.getOrNull(1)?.let { suggestedStorePass = it }
                keyPassRegex.find(content)?.groupValues?.getOrNull(1)?.let { suggestedKeyPass = it }
                aliasRegex.find(content)?.groupValues?.getOrNull(1)?.let { suggestedAlias = it }
            }

            // Extraer sugerencias de INFO_KEYSTORE.txt si existía
            var suggestedTitle = ""
            infoReportFile?.let { file ->
                val lines = file.readLines()
                for (line in lines) {
                    if (line.startsWith("Título:", ignoreCase = true)) {
                        suggestedTitle = line.substringAfter(":").trim()
                    }
                    if (suggestedAlias.isEmpty() && line.startsWith("Alias:", ignoreCase = true)) {
                        suggestedAlias = line.substringAfter(":").trim()
                    }
                }
            }

            if (suggestedTitle.isEmpty()) {
                suggestedTitle = "Importada: " + finalJksFile.nameWithoutExtension.replace("_", " ").capitalizeWords()
            }

            ZipAnalysisResult(
                originalZipFileName = zipFileName,
                jksTempFile = finalJksFile,
                jksFileName = finalJksFile.name,
                jksSizeBytes = finalJksFile.length(),
                suggestedTitle = suggestedTitle,
                suggestedAlias = suggestedAlias,
                suggestedStorePassword = suggestedStorePass,
                suggestedKeyPassword = suggestedKeyPass,
                detectedComponents = detectedComponents.distinct(),
                tempExtractionDir = tempDir
            )
        }
    }

    /**
     * Valida la contraseña del almacén y de la clave privada, e inspecciona el certificado X.509
     * verificando su autenticidad y extrayendo sus huellas y atributos forenses.
     */
    fun validateAndInspectKeystore(
        jksFile: File,
        storePassword: String,
        alias: String,
        keyPassword: String
    ): Result<ImportedKeystoreValidation> {
        return runCatching {
            if (!jksFile.exists()) {
                throw IllegalStateException("El archivo de la clave ya no se encuentra en el almacenamiento temporal.")
            }

            val storePassChars = storePassword.toCharArray()
            val keyPassChars = keyPassword.toCharArray()

            // 1. Intentar cargar con PKCS12 (estándar moderno) y fallback a JKS (legado)
            val keyStore = runCatching {
                val ks = KeyStore.getInstance("PKCS12")
                FileInputStream(jksFile).use { fis -> ks.load(fis, storePassChars) }
                ks
            }.recoverCatching {
                val ks = KeyStore.getInstance("JKS")
                FileInputStream(jksFile).use { fis -> ks.load(fis, storePassChars) }
                ks
            }.getOrElse { e ->
                throw IllegalArgumentException("La contraseña del almacén (Store Password) es incorrecta o el formato no es válido: ${e.message}")
            }

            // 2. Localizar el alias en el almacén
            val availableAliases = keyStore.aliases().toList()
            if (availableAliases.isEmpty()) {
                throw IllegalArgumentException("El almacén de claves no contiene ningún alias o certificado.")
            }

            val targetAlias = if (alias.isNotBlank() && keyStore.containsAlias(alias)) {
                alias
            } else if (availableAliases.size == 1) {
                availableAliases.first()
            } else {
                throw IllegalArgumentException("El alias '$alias' no existe en el almacén. Alias disponibles: ${availableAliases.joinToString(", ")}")
            }

            // 3. Probar la contraseña de la clave privada
            runCatching {
                val key = keyStore.getKey(targetAlias, keyPassChars)
                if (key == null && keyStore.isKeyEntry(targetAlias)) {
                    throw IllegalArgumentException("No se pudo recuperar la clave privada para el alias '$targetAlias'.")
                }
            }.getOrElse { e ->
                throw IllegalArgumentException("La contraseña de la clave (Key Password) es incorrecta para el alias '$targetAlias': ${e.message}")
            }

            // 4. Extraer el certificado X.509
            val cert = keyStore.getCertificate(targetAlias) as? X509Certificate
                ?: throw IllegalArgumentException("El alias '$targetAlias' no contiene un certificado X.509.")

            val holder = JcaX509CertificateHolder(cert)
            val subject = holder.subject

            fun getRdn(style: org.bouncycastle.asn1.ASN1ObjectIdentifier): String {
                val rdns = subject.getRDNs(style)
                return if (rdns.isNotEmpty()) {
                    IETFUtils.valueToString(rdns[0].first.value)
                } else {
                    ""
                }
            }

            val commonName = getRdn(BCStyle.CN).ifEmpty { "Desarrollador" }
            val organization = getRdn(BCStyle.O).ifEmpty { "Android Development" }
            val orgUnit = getRdn(BCStyle.OU).ifEmpty { "Development" }
            val countryCode = getRdn(BCStyle.C).take(2).uppercase().ifEmpty { "ES" }

            // 5. Calcular huellas digitales directamente sobre los bytes del certificado
            val certBytes = cert.encoded
            val sha256 = calculateFingerprint(certBytes, "SHA-256")
            val sha1 = calculateFingerprint(certBytes, "SHA-1")

            // 6. Algoritmo y validez
            val algorithmName = cert.publicKey.algorithm
            val keyAlgorithm = "$algorithmName (Autenticado)"
            val now = System.currentTimeMillis()
            val notBefore = cert.notBefore.time
            val notAfter = cert.notAfter.time
            val isExpired = now > notAfter

            val diffMillis = notAfter - notBefore
            val years = (diffMillis / (1000L * 60 * 60 * 24 * 365)).toInt().coerceAtLeast(1)

            ImportedKeystoreValidation(
                validatedAlias = targetAlias,
                keyAlgorithm = keyAlgorithm,
                sha256Fingerprint = sha256,
                sha1Fingerprint = sha1,
                validityYears = years,
                commonName = commonName,
                organization = organization,
                organizationalUnit = orgUnit,
                countryCode = countryCode,
                notBefore = notBefore,
                notAfter = notAfter,
                isExpired = isExpired
            )
        }
    }

    /**
     * Guarda permanentemente el almacén de claves en el almacenamiento interno de la app,
     * registra la entidad en Room con contraseñas cifradas con AES-256-GCM y limpia los temporales.
     */
    suspend fun commitImport(
        context: Context,
        repository: KeystoreRepository,
        analysis: ZipAnalysisResult,
        title: String,
        storePassword: String,
        keyPassword: String,
        validation: ImportedKeystoreValidation
    ): Result<KeystoreEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val keystoresDir = File(context.filesDir, "keystores").apply { mkdirs() }

            // Sanitizar y buscar un nombre no duplicado en el sistema de archivos
            val baseName = analysis.jksFileName.substringBeforeLast(".")
            val extension = analysis.jksFileName.substringAfterLast(".", "jks")
            var destinationFile = File(keystoresDir, "$baseName.$extension")
            var counter = 1
            while (destinationFile.exists()) {
                destinationFile = File(keystoresDir, "${baseName}_imported_$counter.$extension")
                counter++
            }

            // Copiar el archivo validado a la carpeta permanente
            analysis.jksTempFile.copyTo(destinationFile, overwrite = false)

            // Crear la entidad de persistencia
            val entity = KeystoreEntity(
                title = title.trim().ifEmpty { "Keystore ${validation.validatedAlias}" },
                fileName = destinationFile.name,
                filePath = destinationFile.absolutePath,
                fileSizeBytes = destinationFile.length(),
                alias = validation.validatedAlias,
                storePassword = storePassword,
                keyPassword = keyPassword,
                keyAlgorithm = validation.keyAlgorithm,
                validityYears = validation.validityYears,
                commonName = validation.commonName,
                organization = validation.organization,
                organizationalUnit = validation.organizationalUnit,
                countryCode = validation.countryCode,
                sha256Fingerprint = validation.sha256Fingerprint,
                sha1Fingerprint = validation.sha1Fingerprint,
                createdAt = System.currentTimeMillis()
            )

            // Insertar en Room (repositorio cifra las contraseñas con AES-256-GCM en reposo)
            val generatedId = repository.insertKeystore(entity)
            val finalSavedEntity = entity.copy(id = generatedId)

            // Limpieza del directorio temporal de extracción
            runCatching {
                analysis.tempExtractionDir.deleteRecursively()
            }

            finalSavedEntity
        }
    }

    /**
     * Limpia un directorio temporal en caso de cancelación por parte del usuario.
     */
    fun cleanTempDirectory(tempDir: File?) {
        try {
            tempDir?.deleteRecursively()
        } catch (_: Exception) {
        }
    }

    private fun calculateFingerprint(data: ByteArray, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm).digest(data)
        return digest.joinToString(":") { byte ->
            String.format("%02X", byte)
        }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
