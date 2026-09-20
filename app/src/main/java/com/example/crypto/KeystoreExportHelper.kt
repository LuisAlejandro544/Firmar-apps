package com.example.crypto

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.model.KeystoreEntity
import java.io.File

/**
 * Utilidad para exportar y compartir archivos de Keystore, conversión a Base64 para CI/CD,
 * y copiado seguro al portapapeles con limpieza automática a los 2 minutos (exclusivo para
 * datos copiados desde esta app).
 */
object KeystoreExportHelper {

    // Handler global atado al hilo principal de la aplicación para programar la limpieza a los 2 minutos (120,000 ms)
    private val autoClearHandler = Handler(Looper.getMainLooper())
    private var pendingClearRunnable: Runnable? = null

    // Prefijo y etiqueta distintiva para identificar qué clips fueron copiados exclusivamente por nuestra app
    private const val APP_CLIP_LABEL_PREFIX = "KeyStudio: "
    private const val APP_CLIP_TAG_KEY = "com.example.IS_KEYSTORE_APP_CLIP"
    const val AUTO_CLEAR_DELAY_MILLIS = 2 * 60 * 1000L // 2 minutos exactos (120 segundos)

    /**
     * Comparte el archivo físico del Keystore a través del FileProvider del sistema
     * para que pueda abrirse con MT Manager, gestores de archivos, guardarse en descargas o enviarse.
     */
    fun shareKeystoreFile(context: Context, keystore: KeystoreEntity) {
        try {
            val file = File(keystore.filePath)
            if (!file.exists()) {
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
        } catch (_: Exception) {
            // Manejado silenciosamente; la UI presenta el estado correspondiente
        }
    }

    /**
     * Copia texto al portapapeles de Android y programa su eliminación automática exactamente a los 2 minutos.
     * 
     * Garantía de seguridad estricta:
     * El temporizador de 2 minutos SOLO elimina el portapapeles SI todavía contiene exactamente el texto
     * copiado por esta aplicación (o la etiqueta distintiva de la app). Si el usuario copió cualquier
     * otra cosa de WhatsApp, navegador o notas después de usar nuestra app, NO se borrará el portapapeles
     * para no interferir con sus otras tareas.
     */
    fun copyToClipboard(context: Context, label: String, text: String, isSensitive: Boolean = false) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val distinctLabel = "$APP_CLIP_LABEL_PREFIX$label"
        val clip = ClipData.newPlainText(distinctLabel, text)

        // Metadata de seguridad para identificar autoría exclusiva de nuestra aplicación
        val extras = PersistableBundle().apply {
            putBoolean(APP_CLIP_TAG_KEY, true)
            putString("com.example.CLIP_SOURCE", "KEYSTORE_STUDIO")
            putLong("com.example.COPY_TIMESTAMP", System.currentTimeMillis())
            // En Android 13+ indicamos al sistema que oculte el popup nativo de contenido sensible
            if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clip.description.extras = extras

        clipboard.setPrimaryClip(clip)

        // Programar eliminación segura tras 2 minutos (120,000 ms)
        scheduleAutoClear(context.applicationContext, expectedText = text, expectedLabel = distinctLabel)
    }

    /**
     * Programa la limpieza automática en 2 minutos.
     * Al cumplirse los 2 minutos, inspecciona el portapapeles actual y SOLO lo vacía si
     * coincide con el contenido y las marcas exclusivas de nuestra app.
     */
    private fun scheduleAutoClear(appContext: Context, expectedText: String, expectedLabel: String) {
        // Cancelamos cualquier temporizador previo si el usuario copió otro dato antes
        pendingClearRunnable?.let { autoClearHandler.removeCallbacks(it) }

        val clearTask = Runnable {
            try {
                val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return@Runnable
                val currentClip = clipboard.primaryClip ?: return@Runnable

                // Verificar 1: ¿Tiene nuestra etiqueta de clip distintiva o metadata?
                val description = currentClip.description
                val hasOurMetadata = description?.extras?.getBoolean(APP_CLIP_TAG_KEY, false) == true
                val hasOurLabel = description?.label?.toString() == expectedLabel

                // Verificar 2: ¿El texto coincide exactamente con lo que copió el usuario desde nuestra app?
                val currentText = if (currentClip.itemCount > 0) {
                    currentClip.getItemAt(0)?.text?.toString()
                } else null

                val isOurContent = (hasOurMetadata || hasOurLabel) && (currentText == expectedText)

                // SOLO eliminar si sigue siendo nuestro contenido copiado
                if (isOurContent) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboard.clearPrimaryClip()
                    } else {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            } catch (_: Exception) {
                // Manejo silencioso de posibles restricciones de contexto
            }
        }

        pendingClearRunnable = clearTask
        autoClearHandler.postDelayed(clearTask, AUTO_CLEAR_DELAY_MILLIS)
    }

    /**
     * Convierte el archivo físico de la Keystore a una cadena Base64 (NO_WRAP).
     * Ideal para su uso en variables de entorno de CI/CD como ANDROID_KEYSTORE_BASE64 en GitHub Actions.
     */
    fun generateBase64(keystore: KeystoreEntity): Result<String> {
        return try {
            val file = File(keystore.filePath)
            if (!file.exists()) {
                return Result.failure(IllegalStateException("El archivo .jks no existe en la ruta especificada"))
            }
            val bytes = file.readBytes()
            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Result.success(base64String)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Comparte la cadena Base64 como texto plano mediante el selector de aplicaciones.
     */
    fun shareBase64Text(context: Context, keystore: KeystoreEntity, base64Text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Base64 de ${keystore.fileName}")
                putExtra(Intent.EXTRA_TEXT, base64Text)
            }
            val chooser = Intent.createChooser(intent, "Compartir Base64 de ${keystore.fileName}")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }

    /**
     * Guarda y comparte la cadena Base64 como un archivo físico .base64 mediante FileProvider.
     */
    fun shareBase64AsFile(context: Context, keystore: KeystoreEntity, base64Text: String) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val base64File = File(exportDir, "${keystore.fileName}.base64")
            base64File.writeText(base64Text)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                base64File
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Archivo Base64: ${base64File.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Exportar archivo ${base64File.name}")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
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

    /**
     * Genera un fragmento de ejemplo para descifrar el Base64 en GitHub Actions CI/CD.
     */
    fun generateGitHubActionsYamlSnippet(keystore: KeystoreEntity): String {
        return """
        # Paso para decodificar la Keystore en GitHub Actions (.github/workflows/build.yml)
        - name: Decodificar Keystore de Base64
          run: |
            echo "${'$'}{{ secrets.ANDROID_KEYSTORE_BASE64 }}" | base64 --decode > app/${keystore.fileName}
        """.trimIndent()
    }

    /**
     * Genera un archivo de flujo de trabajo completo y funcional para GitHub Actions (.github/workflows/build-and-sign.yml).
     * Incluye todos los pasos necesarios pre-configurados específicamente para esta llave:
     * decodificación de Base64, inyección de credenciales, compilación con Gradle y publicación de artefactos.
     */
    fun generateFullGitHubActionWorkflow(keystore: KeystoreEntity): String {
        return """
        name: Compilar y Firmar APK Android

        on:
          push:
            branches: [ main, master ]
          pull_request:
            branches: [ main, master ]
          workflow_dispatch:

        jobs:
          build-and-sign:
            name: Compilar y Firmar APK
            runs-on: ubuntu-latest

            steps:
              - name: Clonar código fuente
                uses: actions/checkout@v4

              - name: Configurar Java JDK 17
                uses: actions/setup-java@v4
                with:
                  distribution: 'temurin'
                  java-version: '17'
                  cache: 'gradle'

              - name: Dar permisos de ejecución a Gradle Wrapper
                run: chmod +x ./gradlew

              # Decodifica la clave exportada en Base64 desde los secretos del repositorio
              - name: Restaurar archivo de firma (${keystore.fileName})
                env:
                  KEYSTORE_BASE64: ${'$'}{{ secrets.ANDROID_KEYSTORE_BASE64 }}
                run: |
                  mkdir -p app
                  echo "${'$'}KEYSTORE_BASE64" | base64 --decode > app/${keystore.fileName}

              # Compila el APK de release inyectando los parámetros pre-configurados de esta llave
              - name: Compilar APK con Gradle
                env:
                  KEYSTORE_FILE: ${keystore.fileName}
                  KEYSTORE_PASSWORD: ${'$'}{{ secrets.KEYSTORE_PASSWORD }}
                  KEY_ALIAS: ${keystore.alias}
                  KEY_PASSWORD: ${'$'}{{ secrets.KEY_PASSWORD }}
                run: ./gradlew assembleRelease --stacktrace

              # Firma complementaria con apksigner en caso de generar APK no firmado
              - name: Verificar y Firmar APK con apksigner
                run: |
                  RELEASE_DIR="app/build/outputs/apk/release"
                  UNSIGNED_APK="${'$'}RELEASE_DIR/app-release-unsigned.apk"
                  SIGNED_APK="${'$'}RELEASE_DIR/app-release-signed.apk"
                  
                  if [ -f "${'$'}UNSIGNED_APK" ]; then
                    echo "Firmando APK no alineado con apksigner..."
                    ${'$'}ANDROID_HOME/build-tools/34.0.0/apksigner sign \
                      --ks "app/${keystore.fileName}" \
                      --ks-key-alias "${keystore.alias}" \
                      --ks-pass "pass:${'$'}{{ secrets.KEYSTORE_PASSWORD }}" \
                      --key-pass "pass:${'$'}{{ secrets.KEY_PASSWORD }}" \
                      --out "${'$'}SIGNED_APK" \
                      "${'$'}UNSIGNED_APK"
                  fi

              # Sube el APK compilado y firmado para su descarga directa desde GitHub
              - name: Subir APK firmado como artefacto descargable
                uses: actions/upload-artifact@v4
                with:
                  name: apk-release-firmado
                  path: app/build/outputs/apk/release/*.apk
                  retention-days: 14
        """.trimIndent()
    }

    /**
     * Escribe el archivo binario de la Keystore (.jks o .keystore) directamente en un Uri destino
     * seleccionado por el usuario mediante el Storage Access Framework (SAF) nativo de Android.
     */
    fun writeKeystoreToUri(context: Context, keystore: KeystoreEntity, destinationUri: Uri): Result<Unit> {
        return runCatching {
            val file = File(keystore.filePath)
            if (!file.exists()) {
                throw IllegalStateException("El archivo original no existe en ${keystore.filePath}")
            }
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                outputStream.flush()
            } ?: throw IllegalStateException("No se pudo abrir el flujo de escritura para la URI seleccionada.")
        }
    }

    /**
     * Escribe la cadena Base64 generada directamente como archivo de texto (.base64) en un Uri destino
     * mediante el Storage Access Framework (SAF).
     */
    fun writeBase64ToUri(context: Context, base64Text: String, destinationUri: Uri): Result<Unit> {
        return StorageCompressionHelper.writeBytesToUri(
            context = context,
            destinationUri = destinationUri,
            data = base64Text.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Escribe el bloque de Gradle Kotlin DSL (.gradle.kts) directamente en un Uri destino con SAF.
     */
    fun writeGradleSnippetToUri(context: Context, keystore: KeystoreEntity, destinationUri: Uri): Result<Unit> {
        val snippet = generateGradleKtsSnippet(keystore)
        return StorageCompressionHelper.writeBytesToUri(
            context = context,
            destinationUri = destinationUri,
            data = snippet.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Escribe el archivo YAML del workflow de GitHub Actions (.yml) directamente en un Uri destino con SAF.
     */
    fun writeGitHubWorkflowToUri(context: Context, keystore: KeystoreEntity, destinationUri: Uri): Result<Unit> {
        val workflow = generateFullGitHubActionWorkflow(keystore)
        return StorageCompressionHelper.writeBytesToUri(
            context = context,
            destinationUri = destinationUri,
            data = workflow.toByteArray(Charsets.UTF_8)
        )
    }
}
