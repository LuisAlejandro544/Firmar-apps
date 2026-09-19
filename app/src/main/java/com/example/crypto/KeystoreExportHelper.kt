package com.example.crypto

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.KeystoreEntity
import java.io.File

/**
 * Utilidad para exportar y compartir archivos de Keystore, así como copiar
 * fragmentos de configuración de firma (Gradle/apksigner) y credenciales al portapapeles.
 */
object KeystoreExportHelper {

    /**
     * Comparte el archivo físico del Keystore a través del FileProvider del sistema
     * para que pueda abrirse con MT Manager, gestores de archivos, guardarse en descargas o enviarse.
     */
    fun shareKeystoreFile(context: Context, keystore: KeystoreEntity) {
        try {
            val file = File(keystore.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "El archivo no existe en el almacenamiento", Toast.LENGTH_SHORT).show()
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Keystore: ${keystore.fileName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartir Keystore ${keystore.fileName}")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copia texto al portapapeles de Android y muestra un aviso al usuario.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    /**
     * Genera el bloque de configuración de firma para Gradle Kotlin DSL (build.gradle.kts).
     */
    fun generateGradleKtsSnippet(keystore: KeystoreEntity): String {
        return """
        // Bloque de firma para app/build.gradle.kts
        signingConfigs {
            create("release") {
                storeFile = file("${keystore.fileName}")
                storePassword = "${keystore.storePassword}"
                keyAlias = "${keystore.alias}"
                keyPassword = "${keystore.keyPassword}"
            }
        }
        """.trimIndent()
    }

    /**
     * Genera el comando de apksigner para firmar un APK desde terminal o Termux en el móvil.
     */
    fun generateApkSignerCommand(keystore: KeystoreEntity): String {
        return "apksigner sign --ks ${keystore.fileName} --ks-key-alias ${keystore.alias} --ks-pass pass:${keystore.storePassword} --key-pass pass:${keystore.keyPassword} app-release-unsigned.apk"
    }
}
