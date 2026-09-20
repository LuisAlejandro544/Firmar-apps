package com.example.crypto

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.model.KeystoreEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * Formatos estándar admitidos para la exportación de certificados públicos X.509.
 */
enum class CertificateFormat(
    val extension: String,
    val mimeType: String,
    val label: String,
    val description: String
) {
    PEM(
        extension = "pem",
        mimeType = "application/x-pem-file",
        label = "PEM (.pem)",
        description = "Formato de texto ASCII con encabezados RFC 7468. Ideal para servidores web, consolas cloud y APIs."
    ),
    CRT(
        extension = "crt",
        mimeType = "application/x-x509-ca-cert",
        label = "CRT (.crt)",
        description = "Certificado estándar X.509. Usado comúnmente en Android, Linux y validadores de identidad."
    ),
    DER(
        extension = "der",
        mimeType = "application/x-x509-ca-cert",
        label = "DER (.der)",
        description = "Codificación binaria ASN.1 en crudo. Utilizado en sistemas embebidos, Java y tarjetas inteligentes."
    )
}

/**
 * Motor modular para la extracción, conversión y exportación de certificados públicos X.509
 * a partir de almacenes Keystore (formato PKCS12 o JKS).
 *
 * Garantía de Privacidad y Seguridad Criptográfica:
 * Este módulo SOLO extrae y manipula el certificado público (`X509Certificate`).
 * La clave privada (`PrivateKey`) y las contraseñas del almacén jamás se exportan, comparten ni
 * se incluyen en los archivos generados, haciendo que el resultado sea 100% seguro para compartir.
 */
object CertificateExportHelper {

    /**
     * Extrae el certificado X.509 asociado al alias de la keystore cargada en el dispositivo.
     */
    fun extractCertificate(keystore: KeystoreEntity): Result<X509Certificate> {
        return try {
            val file = File(keystore.filePath)
            if (!file.exists()) {
                return Result.failure(IllegalStateException("El archivo de la keystore no existe en ${keystore.filePath}"))
            }
            extractCertificateFromFile(
                file = file,
                storePassword = keystore.storePassword,
                alias = keystore.alias
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Carga un archivo físico de Keystore (PKCS12 con fallback a JKS) y obtiene su X509Certificate.
     */
    fun extractCertificateFromFile(
        file: File,
        storePassword: String,
        alias: String
    ): Result<X509Certificate> {
        return try {
            val passwordChars = storePassword.toCharArray()

            // Intentar primero con PKCS12 (estándar moderno de Android) y fallback a JKS si es legado
            val keyStore = runCatching {
                val ks = KeyStore.getInstance("PKCS12")
                FileInputStream(file).use { fis -> ks.load(fis, passwordChars) }
                ks
            }.recoverCatching {
                val ks = KeyStore.getInstance("JKS")
                FileInputStream(file).use { fis -> ks.load(fis, passwordChars) }
                ks
            }.getOrThrow()

            // Buscar certificado por el alias especificado o por el primer alias disponible
            val targetAlias = if (keyStore.containsAlias(alias)) {
                alias
            } else {
                keyStore.aliases().asSequence().firstOrNull()
                    ?: throw IllegalStateException("El almacén no contiene ningún alias o certificado")
            }

            val cert = keyStore.getCertificate(targetAlias) as? X509Certificate
                ?: throw IllegalStateException("El alias '$targetAlias' no contiene un certificado X.509 válido")

            Result.success(cert)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convierte el certificado X.509 a texto PEM estándar (RFC 7468).
     * El texto incluye los delimitadores '-----BEGIN CERTIFICATE-----' y saltos de línea de 64 caracteres.
     */
    fun formatAsPem(cert: X509Certificate): String {
        val base64Encoded = Base64.encodeToString(cert.encoded, Base64.NO_WRAP)
        return buildString {
            appendLine("-----BEGIN CERTIFICATE-----")
            base64Encoded.chunked(64).forEach { line ->
                appendLine(line)
            }
            append("-----END CERTIFICATE-----")
        }
    }

    /**
     * Obtiene los bytes binarios en formato DER (codificación ASN.1 en crudo).
     */
    fun formatAsDer(cert: X509Certificate): ByteArray {
        return cert.encoded
    }

    /**
     * Genera los bytes listos para guardar o compartir según el formato elegido.
     */
    fun exportCertificateData(cert: X509Certificate, format: CertificateFormat): ByteArray {
        return when (format) {
            CertificateFormat.PEM -> formatAsPem(cert).toByteArray(Charsets.UTF_8)
            CertificateFormat.CRT, CertificateFormat.DER -> formatAsDer(cert)
        }
    }

    /**
     * Genera el nombre de archivo sugerido para la exportación (ej: "upload_cert.pem").
     */
    fun getSuggestedFileName(keystore: KeystoreEntity, format: CertificateFormat): String {
        val safeAlias = keystore.alias.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return "${safeAlias}_cert.${format.extension}"
    }

    /**
     * Comparte el archivo físico del certificado a través del FileProvider del sistema
     * para que pueda abrirse con gestores de archivos, guardarse en descargas o enviarse por correo/mensajería.
     */
    fun shareCertificate(
        context: Context,
        keystore: KeystoreEntity,
        format: CertificateFormat
    ): Result<Unit> {
        return try {
            val certResult = extractCertificate(keystore)
            if (certResult.isFailure) {
                return Result.failure(certResult.exceptionOrNull() ?: Exception("Error extrayendo certificado"))
            }
            val cert = certResult.getOrThrow()
            val data = exportCertificateData(cert, format)

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileName = getSuggestedFileName(keystore, format)
            val certFile = File(exportDir, fileName)

            FileOutputStream(certFile).use { fos ->
                fos.write(data)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                certFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Certificado Público: $fileName")
                putExtra(Intent.EXTRA_TEXT, "Certificado público X.509 (${format.label}) generado para el alias '${keystore.alias}'.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartir Certificado $fileName")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escribe los bytes del certificado en un Uri destino seleccionado por el usuario
     * mediante el Storage Access Framework (SAF) de Android.
     */
    fun writeCertificateToUri(
        context: Context,
        destinationUri: Uri,
        data: ByteArray
    ): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                outputStream.write(data)
                outputStream.flush()
            } ?: return Result.failure(IllegalStateException("No se pudo abrir el flujo de escritura para la URI"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
