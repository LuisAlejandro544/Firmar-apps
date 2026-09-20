package com.example.crypto

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.KeystoreEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Representa un archivo individual que formará parte de un paquete comprimido ZIP.
 *
 * @param fileName Nombre del archivo dentro del archivo comprimido (ej. "release.jks", "info.txt").
 * @param data Contenido en bytes del archivo.
 * @param comment Comentario opcional descriptivo de la entrada.
 */
data class ZipEntryData(
    val fileName: String,
    val data: ByteArray,
    val comment: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ZipEntryData
        if (fileName != other.fileName) return false
        if (!data.contentEquals(other.data)) return false
        return comment == other.comment
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + (comment?.hashCode() ?: 0)
        return result
    }
}

/**
 * Motor modular de compresión de almacenamiento y empaquetado ultra-optimizado.
 *
 * Proporciona:
 * 1. Algoritmos de compresión en reposo (GZIP / Deflater con nivel 9 de compresión máxima).
 * 2. Creación de paquetes completos "All-in-One" en formato .zip conteniendo la llave,
 *    certificados públicos X.509, cadenas Base64 para CI/CD, scripts de GitHub Actions,
 *    configuración de Gradle y reportes forenses de auditoría.
 * 3. Integración con el Storage Access Framework (SAF) de Android para guardar
 *    cualquier artefacto individual o el paquete comprimido en cualquier carpeta
 *    del administrador de archivos nativo del teléfono.
 * 4. Compatibilidad 100% nativa con procesadores de 32 bits (armeabi-v7a) y 64 bits (arm64-v8a).
 */
object StorageCompressionHelper {

    /**
     * Comprime un arreglo de bytes utilizando el algoritmo DEFLATE con nivel 9 (máxima compresión).
     *
     * @param input Bytes originales sin comprimir.
     * @return Arreglo de bytes comprimidos al máximo.
     */
    fun compressWithMaxDeflate(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(input)
        deflater.finish()

        val outputStream = ByteArrayOutputStream(input.size)
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        deflater.end()
        return outputStream.toByteArray()
    }

    /**
     * Comprime una cadena de texto (como Base64, PEM o YAML) usando GZIP al máximo nivel.
     * Es ideal para reducir cadenas largas de texto a una fracción de su peso.
     */
    fun compressTextGzip(text: String): ByteArray {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val byteStream = ByteArrayOutputStream()
        GZIPOutputStream(byteStream).use { gzip ->
            gzip.write(bytes)
            gzip.finish()
        }
        return byteStream.toByteArray()
    }

    /**
     * Descomprime un arreglo de bytes comprimido con GZIP y retorna la cadena UTF-8 original.
     */
    fun decompressTextGzip(compressedBytes: ByteArray): String {
        val byteStream = ByteArrayInputStream(compressedBytes)
        return GZIPInputStream(byteStream).use { gzip ->
            gzip.bufferedReader(Charsets.UTF_8).readText()
        }
    }

    /**
     * Calcula el porcentaje de ahorro de espacio logrado por la compresión.
     *
     * @param originalSize Tamaño en bytes original.
     * @param compressedSize Tamaño en bytes comprimido.
     * @return Porcentaje de ahorro (ej. 58.4f para un 58.4% de reducción).
     */
    fun calculateSavingsPercentage(originalSize: Long, compressedSize: Long): Float {
        if (originalSize <= 0) return 0f
        val saved = originalSize - compressedSize
        return if (saved > 0) {
            (saved.toFloat() / originalSize.toFloat()) * 100f
        } else {
            0f
        }
    }

    /**
     * Genera un archivo ZIP en memoria con compresión ultra-alta (nivel 9 - BEST_COMPRESSION),
     * empaquetando múltiples entradas de archivos.
     *
     * @param entries Lista de entradas que contendrá el archivo ZIP.
     * @return Arreglo de bytes del archivo .zip listo para guardar o transmitir.
     */
    fun createUltraCompressedZip(entries: List<ZipEntryData>): ByteArray {
        val byteStream = ByteArrayOutputStream()
        ZipOutputStream(byteStream).use { zipStream ->
            // Forzar compresión máxima para reducir el archivo a su mínima expresión
            zipStream.setLevel(Deflater.BEST_COMPRESSION)
            zipStream.setMethod(ZipOutputStream.DEFLATED)

            for (entry in entries) {
                val zipEntry = ZipEntry(entry.fileName).apply {
                    if (entry.comment != null) {
                        comment = entry.comment
                    }
                    time = System.currentTimeMillis()
                }
                zipStream.putNextEntry(zipEntry)
                zipStream.write(entry.data)
                zipStream.closeEntry()
            }
            zipStream.finish()
        }
        return byteStream.toByteArray()
    }

    /**
     * Construye el paquete completo "All-in-One" para una Keystore específica.
     *
     * Contiene:
     * 1. Archivo .jks o .keystore original binario.
     * 2. Certificado público en formato PEM (.pem).
     * 3. Certificado público en formato CRT (.crt).
     * 4. Cadena de la llave codificada en Base64 (.base64).
     * 5. Workflow de compilación y firmado automático para GitHub Actions (.github/workflows/build-and-sign.yml).
     * 6. Configuración de Gradle Kotlin DSL (signingConfigs.gradle.kts).
     * 7. Reporte de auditoría y claves técnicas (INFO_KEYSTORE.txt).
     *
     * @param keystore Entidad de la llave seleccionada.
     * @return Arreglo de bytes del paquete ZIP comprimido al máximo.
     */
    fun buildCompleteZipBundle(keystore: KeystoreEntity): Result<ByteArray> {
        return runCatching {
            val entries = mutableListOf<ZipEntryData>()

            // 1. Archivo de la Keystore (.jks o .keystore)
            val keystoreFile = File(keystore.filePath)
            if (keystoreFile.exists()) {
                entries.add(
                    ZipEntryData(
                        fileName = keystore.fileName,
                        data = keystoreFile.readBytes(),
                        comment = "Almacén de claves PKCS12 compatible con Android y apksigner"
                    )
                )
            }

            // 2. Base64 de la Keystore (para secretos de GitHub Actions)
            val base64Result = KeystoreExportHelper.generateBase64(keystore)
            if (base64Result.isSuccess) {
                val base64Text = base64Result.getOrThrow()
                entries.add(
                    ZipEntryData(
                        fileName = "${keystore.fileName}.base64",
                        data = base64Text.toByteArray(Charsets.UTF_8),
                        comment = "Contenido en Base64 para el secreto ANDROID_KEYSTORE_BASE64 en CI/CD"
                    )
                )
            }

            // 3. Certificados públicos X.509 (PEM y CRT)
            val certResult = CertificateExportHelper.extractCertificate(keystore)
            if (certResult.isSuccess) {
                val cert = certResult.getOrThrow()
                val pemText = CertificateExportHelper.formatAsPem(cert)
                val safeAlias = keystore.alias.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")

                entries.add(
                    ZipEntryData(
                        fileName = "${safeAlias}_cert.pem",
                        data = pemText.toByteArray(Charsets.UTF_8),
                        comment = "Certificado público en formato PEM RFC 7468 (sin claves privadas)"
                    )
                )

                entries.add(
                    ZipEntryData(
                        fileName = "${safeAlias}_cert.crt",
                        data = cert.encoded,
                        comment = "Certificado público en formato binario CRT / X.509"
                    )
                )
            }

            // 4. Fragmento de Gradle Kotlin DSL
            val gradleSnippet = KeystoreExportHelper.generateGradleKtsSnippet(keystore)
            entries.add(
                ZipEntryData(
                    fileName = "signingConfigs.gradle.kts",
                    data = gradleSnippet.toByteArray(Charsets.UTF_8),
                    comment = "Configuración para el bloque signingConfigs de app/build.gradle.kts"
                )
            )

            // 5. Pipeline completo de GitHub Actions
            val ghWorkflow = KeystoreExportHelper.generateFullGitHubActionWorkflow(keystore)
            entries.add(
                ZipEntryData(
                    fileName = "github-actions-build-and-sign.yml",
                    data = ghWorkflow.toByteArray(Charsets.UTF_8),
                    comment = "Workflow para automatizar compilación y firma en GitHub Actions"
                )
            )

            // 6. Reporte descriptivo y seguro de la llave (INFO_KEYSTORE.txt)
            val infoReport = buildString {
                appendLine("======================================================")
                appendLine("REPORTE TÉCNICO DE LA CLAVE DE FIRMA (KEYSTORE)")
                appendLine("======================================================")
                appendLine("Título: ${keystore.title}")
                appendLine("Archivo: ${keystore.fileName}")
                appendLine("Alias: ${keystore.alias}")
                appendLine("Algoritmo: ${keystore.keyAlgorithm}")
                appendLine("Validez: ${keystore.validityYears} años")
                appendLine("Titular (CN): ${keystore.commonName}")
                appendLine("Organización (O): ${keystore.organization}")
                appendLine("Unidad (OU): ${keystore.organizationalUnit}")
                if (keystore.city.isNotBlank()) appendLine("Ciudad (L): ${keystore.city}")
                if (keystore.state.isNotBlank()) appendLine("Estado / Provincia (ST): ${keystore.state}")
                appendLine("País (C): ${keystore.countryCode}")
                appendLine("------------------------------------------------------")
                appendLine("HUELLAS DIGITALES (FINGERPRINTS):")
                appendLine("SHA-256: ${keystore.sha256Fingerprint}")
                appendLine("SHA-1:   ${keystore.sha1Fingerprint}")
                appendLine("------------------------------------------------------")
                appendLine("COMANDO PARA FIRMA MANUAL CON APKSIGNER (Móvil / Termux):")
                appendLine(KeystoreExportHelper.generateApkSignerCommand(keystore))
                appendLine("======================================================")
                appendLine("Generado con KeyStudio - 100% en tu dispositivo móvil.")
            }
            entries.add(
                ZipEntryData(
                    fileName = "INFO_KEYSTORE.txt",
                    data = infoReport.toByteArray(Charsets.UTF_8),
                    comment = "Datos informativos de la llave y huellas digitales para Firebase"
                )
            )

            // Generar el archivo ZIP ultra-comprimido
            createUltraCompressedZip(entries)
        }
    }

    /**
     * Escribe un arreglo de bytes en un Uri de destino seleccionado por el usuario
     * mediante el Storage Access Framework (SAF) de Android.
     */
    fun writeBytesToUri(context: Context, destinationUri: Uri, data: ByteArray): Result<Unit> {
        return runCatching {
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                outputStream.write(data)
                outputStream.flush()
            } ?: throw IllegalStateException("No se pudo abrir el descriptor de escritura para la URI seleccionada.")
        }
    }

    /**
     * Comparte el archivo ZIP completo mediante el selector de aplicaciones de Android
     * utilizando el FileProvider seguro del sistema.
     */
    fun shareZipFile(context: Context, fileName: String, zipBytes: ByteArray): Result<Unit> {
        return runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val zipFile = File(exportDir, fileName)

            FileOutputStream(zipFile).use { fos ->
                fos.write(zipBytes)
                fos.flush()
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Paquete de Firma Completo: $fileName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Paquete completo comprimido con la clave de firma, certificados X.509, Base64 y scripts de CI/CD."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartir paquete $fileName")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}
