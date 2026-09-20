package com.example.ui.importzip

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.ZipAnalysisResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla dedicada para la restauración e importación de paquetes ZIP completos de firma.
 *
 * Ofrece un flujo táctil autónomo:
 * 1. Selección segura mediante el Storage Access Framework (SAF) de Android.
 * 2. Descompresión aislada con protección estricta Zip Slip.
 * 3. Auto-detección inteligente de credenciales y componentes (JKS, certificados, Gradle, CI/CD).
 * 4. Validación criptográfica de contraseñas contra el hardware del dispositivo.
 * 5. Ingesta atómica en Room con cifrado AES-256-GCM.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipImportScreen(
    viewModel: ZipImportViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Selector nativo de archivos ZIP mediante Storage Access Framework (cero permisos peligrosos)
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onZipFileSelected(context, it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Restaurar Paquete ZIP",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("zip_import_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Banner informativo de error si ocurrió algún problema
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.errorMessage?.let { errorText ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("zip_import_error_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Error al procesar el archivo",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = errorText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // ESTADO 1: Éxito en la importación
            if (state.importedKeystore != null) {
                ImportSuccessView(
                    keystore = state.importedKeystore!!,
                    onNavigateToDetail = { onNavigateToDetail(state.importedKeystore!!.id) },
                    onNavigateToList = onNavigateToList
                )
            } else if (state.analysisResult == null) {
                // ESTADO 2: Esperando que el usuario elija un archivo ZIP
                SelectZipInitialView(
                    isAnalyzing = state.isAnalyzing,
                    onOpenPicker = {
                        zipPickerLauncher.launch(
                            arrayOf(
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/octet-stream"
                            )
                        )
                    }
                )
            } else {
                // ESTADO 3: Archivo ZIP analizado, mostrar componentes y formulario de confirmación
                ZipPackageAnalysisForm(
                    analysis = state.analysisResult!!,
                    state = state,
                    viewModel = viewModel,
                    context = context,
                    onPickAnother = {
                        zipPickerLauncher.launch(
                            arrayOf(
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/octet-stream"
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Vista inicial que explica el funcionamiento de la restauración y ofrece el botón de selección.
 */
@Composable
private fun SelectZipInitialView(
    isAnalyzing: Boolean,
    onOpenPicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderZip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Restaurar Copia de Seguridad",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Importa cualquier archivo ZIP generado previamente por KeyStudio. El sistema extraerá el almacén .jks, verificará el certificado X.509 y registrará las credenciales con cifrado seguro en tu dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tarjeta de garantías de seguridad y compatibilidad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureRow(text = "Detección inteligente de almacén .jks o .keystore")
                FeatureRow(text = "Recuperación automática de contraseñas de signingConfigs")
                FeatureRow(text = "Reconstrucción automática desde Base64 para CI/CD")
                FeatureRow(text = "Cifrado en reposo AES-256-GCM con Android KeyStore")
                FeatureRow(text = "Auditoría forense de huellas SHA-256 y SHA-1")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onOpenPicker,
                enabled = !isAnalyzing,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("select_zip_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Analizando archivo ZIP...")
                } else {
                    Icon(imageVector = Icons.Default.Archive, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Seleccionar Archivo .ZIP",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Fila descriptiva con viñeta para las características de seguridad.
 */
@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Vista del paquete ZIP una vez analizado, mostrando los archivos detectados y el formulario
 * para confirmar contraseñas e importar a la base de datos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZipPackageAnalysisForm(
    analysis: ZipAnalysisResult,
    state: ZipImportUiState,
    viewModel: ZipImportViewModel,
    context: Context,
    onPickAnother: () -> Unit
) {
    val formattedSize = "%.1f KB".format(analysis.jksSizeBytes / 1024.0)

    // Tarjeta de resumen de componentes detectados en el ZIP
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = analysis.originalZipFileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Archivo: ${analysis.jksFileName} ($formattedSize)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Artefactos reconocidos en el paquete:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                analysis.detectedComponents.forEach { component ->
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text(component, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }

    // Formulario de confirmación y ajuste de credenciales
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Text(
                text = "Confirmación de Credenciales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Verifica los datos de la clave. Si el ZIP contenía la configuración de Gradle, las contraseñas se han precargado automáticamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Campo: Título de la Keystore
            OutlinedTextField(
                value = state.titleInput,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text("Título descriptivo") },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Campo: Alias de la Clave
            OutlinedTextField(
                value = state.aliasInput,
                onValueChange = { viewModel.onAliasChanged(it) },
                label = { Text("Alias de la clave (Key Alias)") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_alias_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Campo: Contraseña del almacén (Store Password)
            OutlinedTextField(
                value = state.storePasswordInput,
                onValueChange = { viewModel.onStorePasswordChanged(it) },
                label = { Text("Contraseña del almacén (Store Password)") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleStorePasswordVisibility() }) {
                        Icon(
                            imageVector = if (state.isStorePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (state.isStorePasswordVisible) "Ocultar" else "Mostrar"
                        )
                    }
                },
                visualTransformation = if (state.isStorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_store_password_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Toggle: Misma contraseña para la clave privada
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Misma contraseña para la clave privada",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = state.isSamePassword,
                    onCheckedChange = { viewModel.onSamePasswordToggle(it) },
                    modifier = Modifier.testTag("import_same_password_switch")
                )
            }

            // Campo: Contraseña de la clave privada (si no es la misma)
            AnimatedVisibility(visible = !state.isSamePassword) {
                OutlinedTextField(
                    value = state.keyPasswordInput,
                    onValueChange = { viewModel.onKeyPasswordChanged(it) },
                    label = { Text("Contraseña de la clave (Key Password)") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleKeyPasswordVisibility() }) {
                            Icon(
                                imageVector = if (state.isKeyPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (state.isKeyPasswordVisible) "Ocultar" else "Mostrar"
                            )
                        }
                    },
                    visualTransformation = if (state.isKeyPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_key_password_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Botón principal de validación e importación
            Button(
                onClick = { viewModel.commitImport(context) },
                enabled = !state.isImporting && state.storePasswordInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_import_button")
            ) {
                if (state.isImporting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Validando e Importando...")
                } else {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Validar e Importar a mi Biblioteca",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Botón secundario para seleccionar otro paquete ZIP
            OutlinedButton(
                onClick = onPickAnother,
                enabled = !state.isImporting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_another_zip_button")
            ) {
                Text("Elegir otro archivo .ZIP")
            }
        }
    }
}

/**
 * Vista de confirmación cuando el paquete ZIP ha sido importado con éxito.
 */
@Composable
private fun ImportSuccessView(
    keystore: com.example.data.model.KeystoreEntity,
    onNavigateToDetail: () -> Unit,
    onNavigateToList: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("import_success_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¡Keystore Importada con Éxito!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "La clave ha sido validada criptográficamente e incorporada a tu biblioteca local. Sus contraseñas han sido protegidas mediante AES-256-GCM en el hardware seguro de tu móvil.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Ficha con detalles técnicos de la clave importada
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailItem(label = "Título", value = keystore.title)
                DetailItem(label = "Archivo guardado", value = keystore.fileName)
                DetailItem(label = "Alias validado", value = keystore.alias)
                DetailItem(label = "Algoritmo", value = keystore.keyAlgorithm)
                DetailItem(label = "Titular (CN)", value = keystore.commonName)
                DetailItem(
                    label = "Huella SHA-256",
                    value = keystore.sha256Fingerprint.take(23) + "..."
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNavigateToDetail,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("view_imported_detail_button")
            ) {
                Text(
                    text = "Ver Detalles y Exportar",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onNavigateToList,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("go_to_list_button")
            ) {
                Text("Ir a Mis Keystores")
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
