package com.example.ui.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.CertificateFormat
import com.example.crypto.KeystoreExportHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla detallada de una keystore específica.
 * Muestra las credenciales completas, huellas SHA-1 / SHA-256,
 * fragmento de Gradle listo para copiar, conversión a Base64 con 1 clic
 * y sistema de notificaciones in-app de seguridad de portapapeles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeystoreDetailScreen(
    keystoreId: Long,
    viewModel: KeystoreDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keystore by viewModel.keystore.collectAsStateWithLifecycle()
    val isStorePassRevealed by viewModel.isStorePasswordRevealed.collectAsStateWithLifecycle()
    val isKeyPassRevealed by viewModel.isKeyPasswordRevealed.collectAsStateWithLifecycle()
    val base64Content by viewModel.base64Content.collectAsStateWithLifecycle()
    val isGeneratingBase64 by viewModel.isGeneratingBase64.collectAsStateWithLifecycle()
    val base64Error by viewModel.base64Error.collectAsStateWithLifecycle()
    val securityAlert by viewModel.securityAlert.collectAsStateWithLifecycle()

    // Estados para exportación de certificados públicos (.pem, .crt, .der)
    val certificatePem by viewModel.certificatePem.collectAsStateWithLifecycle()
    val isExtractingCert by viewModel.isExtractingCert.collectAsStateWithLifecycle()
    val certError by viewModel.certError.collectAsStateWithLifecycle()
    val selectedCertFormat by viewModel.selectedCertFormat.collectAsStateWithLifecycle()

    // Estados para compresión y paquete completo (.zip)
    val isPackagingZip by viewModel.isPackagingZip.collectAsStateWithLifecycle()
    val zipBundleError by viewModel.zipBundleError.collectAsStateWithLifecycle()
    val compressionStats by viewModel.compressionStats.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // Estado para la navegación por pestañas del bloque de códigos de integración (0 = Gradle, 1 = CI/CD)
    var selectedCodeSnippetTab by remember { mutableStateOf(0) }

    // Launcher del Storage Access Framework para guardar directamente el certificado en el almacenamiento
    val saveCertLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(selectedCertFormat.mimeType)
    ) { uri ->
        if (uri != null) {
            viewModel.saveCertificateToUri(context, uri, selectedCertFormat)
        }
    }

    // Launcher SAF para el paquete All-in-One comprimido (.zip)
    val saveZipBundleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.saveZipBundleToUri(context, uri)
        }
    }

    // Launcher SAF para la keystore física (.jks / .keystore) individual
    val saveKeystoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.saveKeystoreToUri(context, uri)
        }
    }

    // Launcher SAF para el archivo Base64 individual (.base64)
    val saveBase64Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.saveBase64ToUri(context, uri)
        }
    }

    // Launcher SAF para el fragmento de Gradle Kotlin DSL (.gradle.kts)
    val saveGradleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.saveGradleSnippetToUri(context, uri)
        }
    }

    // Launcher SAF para el archivo YAML de GitHub Actions (.yml)
    val saveWorkflowLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.saveGitHubWorkflowToUri(context, uri)
        }
    }

    LaunchedEffect(keystoreId) {
        viewModel.loadKeystore(keystoreId)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = keystore?.title ?: "Detalle de Keystore",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar"
                            )
                        }
                    },
                    actions = {
                        keystore?.let { item ->
                            IconButton(
                                onClick = { KeystoreExportHelper.shareKeystoreFile(context, item) },
                                modifier = Modifier.testTag("detail_share_appbar_button")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Compartir archivo")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            val currentKeystore = keystore
            if (currentKeystore == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(currentKeystore.createdAt))
                val formattedSize = "%.1f KB".format(currentKeystore.fileSizeBytes / 1024.0)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tarjeta 1: Archivo y Metadatos Generales
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_file_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Información del Archivo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            DetailItemRow(label = "Nombre de archivo", value = currentKeystore.fileName) {
                                viewModel.copyNonSensitiveValue(context, "Nombre de archivo", currentKeystore.fileName)
                            }
                            DetailItemRow(label = "Tamaño", value = formattedSize)
                            DetailItemRow(label = "Fecha de generación", value = formattedDate)
                            DetailItemRow(label = "Algoritmo y tamaño", value = currentKeystore.keyAlgorithm)
                            DetailItemRow(label = "Validez", value = "${currentKeystore.validityYears} años")
                            if (compressionStats != null) {
                                DetailItemRow(label = "Optimización Deflate", value = compressionStats!!)
                            }
                            DetailItemRow(label = "Ruta interna", value = currentKeystore.filePath) {
                                viewModel.copyNonSensitiveValue(context, "Ruta de archivo", currentKeystore.filePath)
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Text(
                                text = "Distinguished Name X.500 (Google / Android Studio):",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            DetailItemRow(label = "Titular (CN)", value = currentKeystore.commonName) {
                                viewModel.copyNonSensitiveValue(context, "Titular", currentKeystore.commonName)
                            }
                            if (currentKeystore.organization.isNotBlank()) {
                                DetailItemRow(label = "Organización (O)", value = currentKeystore.organization) {
                                    viewModel.copyNonSensitiveValue(context, "Organización", currentKeystore.organization)
                                }
                            }
                            if (currentKeystore.organizationalUnit.isNotBlank()) {
                                DetailItemRow(label = "Unidad (OU)", value = currentKeystore.organizationalUnit) {
                                    viewModel.copyNonSensitiveValue(context, "Unidad", currentKeystore.organizationalUnit)
                                }
                            }
                            if (currentKeystore.city.isNotBlank()) {
                                DetailItemRow(label = "Ciudad o Localidad (L)", value = currentKeystore.city) {
                                    viewModel.copyNonSensitiveValue(context, "Ciudad", currentKeystore.city)
                                }
                            }
                            if (currentKeystore.state.isNotBlank()) {
                                DetailItemRow(label = "Estado o Provincia (ST)", value = currentKeystore.state) {
                                    viewModel.copyNonSensitiveValue(context, "Estado", currentKeystore.state)
                                }
                            }
                            DetailItemRow(label = "Código de País (C)", value = currentKeystore.countryCode) {
                                viewModel.copyNonSensitiveValue(context, "Código de País", currentKeystore.countryCode)
                            }
                        }
                    }

                    // Tarjeta 2: Credenciales de Acceso
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .testTag("detail_credentials_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Credenciales de Acceso",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            DetailItemRow(label = "Alias de la clave", value = currentKeystore.alias) {
                                viewModel.copyNonSensitiveValue(context, "Alias", currentKeystore.alias)
                            }

                            // Password Keystore (sensible -> advertencia de seguridad in-app)
                            val displayStorePass = if (isStorePassRevealed) currentKeystore.storePassword else "••••••••"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Contraseña del Keystore", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(displayStorePass, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Row {
                                    IconButton(onClick = { viewModel.toggleStorePasswordVisibility() }) {
                                        Icon(
                                            imageVector = if (isStorePassRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Revelar contraseña"
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.copySensitiveValue(context, "Contraseña del Keystore", currentKeystore.storePassword)
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar contraseña")
                                    }
                                }
                            }

                            // Password Clave (sensible -> advertencia de seguridad in-app)
                            val displayKeyPass = if (isKeyPassRevealed) currentKeystore.keyPassword else "••••••••"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Contraseña de la Clave", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(displayKeyPass, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Row {
                                    IconButton(onClick = { viewModel.toggleKeyPasswordVisibility() }) {
                                        Icon(
                                            imageVector = if (isKeyPassRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Revelar contraseña"
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.copySensitiveValue(context, "Contraseña de la Clave", currentKeystore.keyPassword)
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar contraseña")
                                    }
                                }
                            }
                        }
                    }

                    // Tarjeta 3: Conversión y Exportación a Base64 con 1 Clic (CI/CD y GitHub Actions)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .testTag("detail_base64_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.DataObject, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Exportación a Base64 (CI/CD)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Convierte el archivo .jks a una cadena Base64 con 1 solo clic. Ideal para GitHub Actions (ANDROID_KEYSTORE_BASE64), GitLab CI o copias de seguridad sin PC.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val base64 = base64Content
                            if (base64 == null) {
                                Button(
                                    onClick = { viewModel.generateBase64() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("generate_base64_button"),
                                    enabled = !isGeneratingBase64
                                ) {
                                    if (isGeneratingBase64) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generando Base64...")
                                    } else {
                                        Icon(Icons.Default.DataObject, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generar Base64 con 1 Clic")
                                    }
                                }

                                if (base64Error != null) {
                                    Text(
                                        text = base64Error ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                // Vista de la cadena Base64 generada
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${base64.length} caracteres · ~${"%.1f".format(base64.length / 1024.0)} KB",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    TextButton(onClick = { viewModel.clearBase64() }) {
                                        Text("Ocultar")
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = base64.take(240) + if (base64.length > 240) "..." else "",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                // Botón principal de copia (sensible -> dispara advertencia in-app)
                                Button(
                                    onClick = {
                                        viewModel.copySensitiveValue(context, "Base64 de la Keystore", base64)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("copy_base64_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copiar Base64 Completo")
                                }

                                // Botones secundarios: Compartir como texto o archivo
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            KeystoreExportHelper.shareBase64Text(context, currentKeystore, base64)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Compartir", fontSize = 13.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            KeystoreExportHelper.shareBase64AsFile(context, currentKeystore, base64)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Compartir .base64", fontSize = 12.sp)
                                    }
                                }

                                // Botón para guardar individualmente el archivo Base64 con SAF
                                OutlinedButton(
                                    onClick = {
                                        saveBase64Launcher.launch(viewModel.getSuggestedBase64FileName())
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Guardar Archivo .base64 en Móvil", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Tarjeta 4: Huellas Digitales del Certificado (SHA-256 y SHA-1)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_fingerprints_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Huellas Digitales del Certificado",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Necesarias para vincular Firebase, Google Sign-In, Play Console y APIs de terceros:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // SHA-256
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SHA-256", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    FilledTonalIconButton(
                                        onClick = {
                                            viewModel.copyNonSensitiveValue(context, "Huella SHA-256", currentKeystore.sha256Fingerprint)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar SHA-256", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(
                                    text = currentKeystore.sha256Fingerprint,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            // SHA-1
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SHA-1", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    FilledTonalIconButton(
                                        onClick = {
                                            viewModel.copyNonSensitiveValue(context, "Huella SHA-1", currentKeystore.sha1Fingerprint)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar SHA-1", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(
                                    text = currentKeystore.sha1Fingerprint,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    // Tarjeta 5: Exportación de Certificados Públicos X.509 (.pem / .crt / .der)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .testTag("detail_public_cert_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Encabezado de la tarjeta
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Certificado Público (.pem / .crt / .der)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Banner de seguridad explicativo
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "100% Seguro para compartir. Contiene únicamente tu firma pública X.509 v3. Tu clave privada y contraseñas jamás se exponen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Selector de formato (PEM, CRT, DER)
                            SecondaryTabRow(
                                selectedTabIndex = selectedCertFormat.ordinal,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("cert_format_tab_row")
                            ) {
                                CertificateFormat.entries.forEach { format ->
                                    Tab(
                                        selected = selectedCertFormat == format,
                                        onClick = { viewModel.setCertificateFormat(format) },
                                        modifier = Modifier.testTag("tab_cert_${format.extension}"),
                                        text = {
                                            Text(
                                                text = format.label,
                                                fontWeight = if (selectedCertFormat == format) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            )
                                        }
                                    )
                                }
                            }

                            // Descripción del formato seleccionado
                            Text(
                                text = selectedCertFormat.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Mensaje de error si falla la extracción
                            if (certError != null) {
                                Text(
                                    text = certError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Contenido específico para formato PEM (Texto)
                            if (selectedCertFormat == CertificateFormat.PEM) {
                                val pem = certificatePem
                                if (pem == null) {
                                    Button(
                                        onClick = { viewModel.loadCertificatePem() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("load_cert_pem_button"),
                                        enabled = !isExtractingCert
                                    ) {
                                        if (isExtractingCert) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Extrayendo Certificado PEM...")
                                        } else {
                                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Ver y Generar Texto PEM")
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Bloque PEM (${pem.lines().size} líneas)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        TextButton(onClick = { viewModel.clearCertificatePem() }) {
                                            Text("Ocultar")
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 160.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                            .verticalScroll(rememberScrollState())
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = pem,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.copyCertificatePem(context) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("copy_cert_pem_button")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Copiar Certificado PEM al Portapapeles")
                                    }
                                }
                            }

                            // Botones de acción: Compartir archivo y Guardar en almacenamiento
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.shareCertificate(context, selectedCertFormat)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("share_cert_${selectedCertFormat.extension}_button"),
                                    enabled = !isExtractingCert
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Compartir .${selectedCertFormat.extension}", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val suggestedName = viewModel.getSuggestedFileName(selectedCertFormat)
                                        saveCertLauncher.launch(suggestedName)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_cert_${selectedCertFormat.extension}_button"),
                                    enabled = !isExtractingCert
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Guardar en Móvil", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Sección de Códigos y Scripts de Integración con Navegación por Pestañas
                    // Permite alternar entre Gradle y GitHub Actions sin tener que hacer scroll vertical innecesario
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .testTag("detail_integration_code_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Cabecera de la sección
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Códigos de Integración y Despliegue",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Selecciona el entorno para ver y copiar el fragmento de configuración listo para tu proyecto:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Selector de pestañas para alternar entre Gradle y CI/CD
                            SecondaryTabRow(
                                selectedTabIndex = selectedCodeSnippetTab,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .testTag("code_snippet_tab_row")
                            ) {
                                Tab(
                                    selected = selectedCodeSnippetTab == 0,
                                    onClick = { selectedCodeSnippetTab = 0 },
                                    modifier = Modifier.testTag("tab_gradle_snippet"),
                                    text = {
                                        Text(
                                            text = "build.gradle.kts",
                                            fontWeight = if (selectedCodeSnippetTab == 0) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                Tab(
                                    selected = selectedCodeSnippetTab == 1,
                                    onClick = { selectedCodeSnippetTab = 1 },
                                    modifier = Modifier.testTag("tab_github_actions"),
                                    text = {
                                        Text(
                                            text = "GitHub Actions CI/CD",
                                            fontWeight = if (selectedCodeSnippetTab == 1) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.IntegrationInstructions,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }

                            // Contenido animado según la pestaña seleccionada
                            AnimatedContent(
                                targetState = selectedCodeSnippetTab,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                            slideOutHorizontally { width -> -width } + fadeOut()
                                        )
                                    } else {
                                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                            slideOutHorizontally { width -> width } + fadeOut()
                                        )
                                    }
                                },
                                label = "CodeSnippetTabContentTransition"
                            ) { targetTab ->
                                if (targetTab == 0) {
                                    // Pestaña 0: Configuración para Gradle Kotlin DSL
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Bloque de signingConfigs para agregar a 'app/build.gradle.kts':",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        val snippet = KeystoreExportHelper.generateGradleKtsSnippet(currentKeystore)

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = snippet,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.copyNonSensitiveValue(context, "Bloque Gradle", snippet)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("copy_gradle_button")
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Copiar Configuración de Gradle")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                saveGradleLauncher.launch(viewModel.getSuggestedGradleFileName())
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Guardar signingConfigs.gradle.kts en Móvil")
                                        }
                                    }
                                } else {
                                    // Pestaña 1: Workflow para GitHub Actions CI/CD
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Pipeline completo listo para producción (.github/workflows/build-and-sign.yml) con los datos de esta llave ya pre-rellenados:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        val ghWorkflow = KeystoreExportHelper.generateFullGitHubActionWorkflow(currentKeystore)

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                .verticalScroll(rememberScrollState())
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = ghWorkflow,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.copyNonSensitiveValue(context, "Workflow GitHub Actions", ghWorkflow)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("copy_github_action_button")
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Copiar Workflow Completo de GitHub Actions")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                saveWorkflowLauncher.launch(viewModel.getSuggestedWorkflowFileName())
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Guardar build-and-sign.yml en Móvil")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tarjeta de Exportación Completa: Paquete All-in-One Comprimido (.zip)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("zip_bundle_export_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Paquete Completo Comprimido (.zip)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Respaldo todo en uno con compresión ultra-alta",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "Empaqueta la llave original (.jks), certificados (.pem y .crt), cadena Base64 (.base64), script de GitHub Actions, configuración de Gradle y reporte en un solo archivo .zip optimizado al máximo. Puedes guardarlo en cualquier carpeta de tu dispositivo o en la nube.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isPackagingZip) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Comprimiendo y empaquetando archivos...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            if (zipBundleError != null) {
                                Text(
                                    text = zipBundleError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.shareZipBundle(context)
                                    },
                                    enabled = !isPackagingZip,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Compartir .zip", fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        saveZipBundleLauncher.launch(viewModel.getSuggestedZipFileName())
                                    },
                                    enabled = !isPackagingZip,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Guardar .zip", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Botones de acción principales para la Keystore original (.jks)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { KeystoreExportHelper.shareKeystoreFile(context, currentKeystore) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("share_file_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compartir Llave", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                saveKeystoreLauncher.launch(currentKeystore.fileName)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("save_keystore_saf_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar .jks", fontSize = 13.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("delete_keystore_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminar esta Keystore")
                    }

                    Spacer(modifier = Modifier.height(64.dp))
                }
            }

            // Diálogo de confirmación para eliminar desde detalle
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("¿Eliminar Keystore?") },
                    text = { Text("Esta acción eliminará permanentemente la keystore del dispositivo.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirm = false
                                viewModel.deleteKeystore(onDeleted = onBack)
                            }
                        ) {
                            Text("Eliminar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }

        // Notificación In-App de Seguridad de la App (en lugar de la notificación fea nativa de Android)
        AnimatedVisibility(
            visible = securityAlert != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            securityAlert?.let { alert ->
                SecurityNotificationBanner(
                    alert = alert,
                    onDismiss = { viewModel.dismissSecurityAlert() }
                )
            }
        }
    }
}

/**
 * Notificación in-app personalizada con diseño Material Design 3 de alta calidad.
 * Informa al usuario sobre el contenido copiado y le advierte sobre los riesgos de privacidad
 * al exponer datos en el portapapeles del teléfono.
 */
@Composable
private fun SecurityNotificationBanner(
    alert: SecurityAlert,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-cierre de la notificación tras 6.5 segundos si el usuario no la descarta manualmente
    LaunchedEffect(alert.id) {
        delay(6500)
        onDismiss()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("in_app_security_notification"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isSensitive) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (alert.isSensitive) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (alert.isSensitive) Icons.Default.Shield else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (alert.isSensitive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (alert.isSensitive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (alert.isSensitive) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.95f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.95f)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar aviso",
                    modifier = Modifier.size(18.dp),
                    tint = if (alert.isSensitive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun DetailItemRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        if (onCopy != null) {
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar $label")
            }
        }
    }
}
