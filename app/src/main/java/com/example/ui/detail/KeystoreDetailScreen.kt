package com.example.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.example.crypto.KeystoreExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla detallada de una keystore específica.
 * Muestra las credenciales completas, huellas SHA-1 / SHA-256,
 * fragmento de Gradle listo para copiar y opciones de exportación directa.
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
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(keystoreId) {
        viewModel.loadKeystore(keystoreId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                            KeystoreExportHelper.copyToClipboard(context, "Nombre de archivo", currentKeystore.fileName)
                        }
                        DetailItemRow(label = "Tamaño", value = formattedSize)
                        DetailItemRow(label = "Fecha de generación", value = formattedDate)
                        DetailItemRow(label = "Algoritmo y tamaño", value = currentKeystore.keyAlgorithm)
                        DetailItemRow(label = "Validez", value = "${currentKeystore.validityYears} años")
                        DetailItemRow(label = "Ruta interna", value = currentKeystore.filePath) {
                            KeystoreExportHelper.copyToClipboard(context, "Ruta de archivo", currentKeystore.filePath)
                        }
                    }
                }

                // Tarjeta 2: Credenciales de Acceso
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            KeystoreExportHelper.copyToClipboard(context, "Alias", currentKeystore.alias)
                        }

                        // Password Keystore
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
                                    KeystoreExportHelper.copyToClipboard(context, "Contraseña Keystore", currentKeystore.storePassword)
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar contraseña")
                                }
                            }
                        }

                        // Password Clave
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
                                    KeystoreExportHelper.copyToClipboard(context, "Contraseña Clave", currentKeystore.keyPassword)
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar contraseña")
                                }
                            }
                        }
                    }
                }

                // Tarjeta 3: Huellas Digitales del Certificado (SHA-256 y SHA-1)
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
                                        KeystoreExportHelper.copyToClipboard(context, "Huella SHA-256", currentKeystore.sha256Fingerprint)
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
                                        KeystoreExportHelper.copyToClipboard(context, "Huella SHA-1", currentKeystore.sha1Fingerprint)
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

                // Tarjeta 4: Configuración de Firma para Gradle
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_gradle_snippet_card"),
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
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Código para build.gradle.kts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

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
                                KeystoreExportHelper.copyToClipboard(context, "Bloque Gradle", snippet)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copiar Configuración de Gradle")
                        }
                    }
                }

                // Botones de acción principales
                FilledTonalButton(
                    onClick = { KeystoreExportHelper.shareKeystoreFile(context, currentKeystore) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("share_file_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartir o Exportar Archivo .JKS")
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

                Spacer(modifier = Modifier.height(32.dp))
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
