package com.example.ui.generator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.crypto.KeystoreExportHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pantalla principal del generador de keystores.
 * Proporciona un formulario intuitivo optimizado para móviles con validación,
 * opciones rápidas y compatibilidad con estándares de firma de Android.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: GeneratorViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta Hero informativa y botón de autorelleno para pruebas rápidas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_banner_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Seguridad",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Crear Nuevo Keystore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Genera archivos JKS/PKCS12 para firmar APKs o AABs de producción.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { viewModel.applyQuickPreset() },
                        modifier = Modifier.testTag("quick_preset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Datos de prueba", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Sección 1: Información Básica
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Archivo y Alias",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = state.title,
                        onValueChange = { viewModel.onTitleChange(it) },
                        label = { Text("Nombre descriptivo") },
                        placeholder = { Text("ej. Mi App Release") },
                        leadingIcon = {
                            Icon(Icons.Default.Title, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("title_input"),
                        singleLine = true
                    )

                    // Selector de formato de archivo (.jks o .keystore)
                    Text(
                        text = "Formato de archivo:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedExtension == ".jks",
                            onClick = { viewModel.onFileExtensionChange(".jks") },
                            label = { Text(".jks (Estándar Android)") },
                            modifier = Modifier.testTag("chip_format_jks")
                        )
                        FilterChip(
                            selected = state.selectedExtension == ".keystore",
                            onClick = { viewModel.onFileExtensionChange(".keystore") },
                            label = { Text(".keystore (Clásico / Flutter)") },
                            modifier = Modifier.testTag("chip_format_keystore")
                        )
                    }

                    // Campo de nombre de archivo con auto-completado de extensión
                    OutlinedTextField(
                        value = state.fileBaseName,
                        onValueChange = { viewModel.onFileBaseNameChange(it) },
                        label = { Text("Nombre del archivo") },
                        placeholder = { Text("ej. release_key") },
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        },
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = state.selectedExtension,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        },
                        supportingText = {
                            Text(
                                text = "Archivo final: ${state.fullFileName} (la extensión se coloca automáticamente)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("filename_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = state.alias,
                        onValueChange = { viewModel.onAliasChange(it) },
                        label = { Text("Alias de la clave") },
                        placeholder = { Text("ej. key0 o release") },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alias_input"),
                        singleLine = true
                    )
                }
            }

            // Sección 2: Credenciales y Contraseñas
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. Seguridad y Contraseñas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = state.storePassword,
                        onValueChange = { viewModel.onStorePasswordChange(it) },
                        label = { Text("Contraseña del Keystore") },
                        placeholder = { Text("Mínimo 6 caracteres") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.toggleStorePasswordVisibility() },
                                modifier = Modifier.testTag("toggle_store_pass_visibility")
                            ) {
                                Icon(
                                    imageVector = if (state.isStorePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Mostrar contraseña"
                                )
                            }
                        },
                        visualTransformation = if (state.isStorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("store_password_input"),
                        singleLine = true
                    )

                    // Switch para usar la misma contraseña (muy cómodo en móvil)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Misma contraseña para la clave",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Recomendado por defecto en Gradle y Android Studio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.useSamePassword,
                            onCheckedChange = { viewModel.onUseSamePasswordToggle(it) },
                            modifier = Modifier.testTag("same_password_switch")
                        )
                    }

                    AnimatedVisibility(
                        visible = !state.useSamePassword,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OutlinedTextField(
                            value = state.keyPassword,
                            onValueChange = { viewModel.onKeyPasswordChange(it) },
                            label = { Text("Contraseña individual de la clave") },
                            placeholder = { Text("Mínimo 6 caracteres") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.toggleKeyPasswordVisibility() },
                                    modifier = Modifier.testTag("toggle_key_pass_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (state.isKeyPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Mostrar contraseña"
                                    )
                                }
                            },
                            visualTransformation = if (state.isKeyPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("key_password_input"),
                            singleLine = true
                        )
                    }
                }
            }

            // Sección 3: Parámetros del Certificado y Clave
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. Parámetros Criptográficos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Tamaño de la clave RSA
                    Text(
                        text = "Tamaño de Clave RSA:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.keySize == 2048,
                            onClick = { viewModel.onKeySizeChange(2048) },
                            label = { Text("RSA 2048 bits (Estándar)") },
                            modifier = Modifier.testTag("chip_rsa_2048")
                        )
                        FilterChip(
                            selected = state.keySize == 4096,
                            onClick = { viewModel.onKeySizeChange(4096) },
                            label = { Text("RSA 4096 bits") },
                            modifier = Modifier.testTag("chip_rsa_4096")
                        )
                    }

                    // Validez con Slider interactivo (1 día a 100 años) y Accesos Rápidos
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Validez del Certificado:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Etiqueta formateada y amigable
                            val validityLabel = remember(state.validityDays) {
                                when {
                                    state.validityDays == 1 -> "1 día"
                                    state.validityDays < 30 -> "${state.validityDays} días"
                                    state.validityDays in 30..364 -> {
                                        val months = state.validityDays / 30
                                        "${state.validityDays} días (~$months ${if (months == 1) "mes" else "meses"})"
                                    }
                                    state.validityDays % 365 == 0 -> {
                                        val years = state.validityDays / 365
                                        "$years ${if (years == 1) "año" else "años"}"
                                    }
                                    else -> {
                                        val years = state.validityDays / 365
                                        val remDays = state.validityDays % 365
                                        "$years a y $remDays d (${state.validityDays} d)"
                                    }
                                }
                            }

                            Text(
                                text = validityLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Fecha de caducidad estimada en vivo
                        val expiryDateText = remember(state.validityDays) {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, state.validityDays)
                            val formatter = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
                            "Caduca aprox.: ${formatter.format(cal.time)}"
                        }
                        Text(
                            text = expiryDateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Slider continuo desde 1 día hasta 36,500 días (100 años)
                        Slider(
                            value = state.validityDays.toFloat(),
                            onValueChange = { viewModel.onValidityDaysChange(it.toInt()) },
                            valueRange = 1f..36500f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("validity_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1 día (Mín)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "100 años (Máx)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Fila de accesos rápidos horizontales
                        Text(
                            text = "Accesos rápidos:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Pair("1 día", 1),
                                Pair("30 días", 30),
                                Pair("1 año", 365),
                                Pair("25 años", 25 * 365),
                                Pair("30 años", 30 * 365),
                                Pair("100 años", 100 * 365)
                            ).forEach { (label, days) ->
                                FilterChip(
                                    selected = state.validityDays == days,
                                    onClick = { viewModel.onValidityDaysChange(days) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("chip_days_$days")
                                )
                            }
                        }
                    }

                    // Botón colapsable para datos de certificado avanzados
                    TextButton(
                        onClick = { viewModel.toggleAdvancedSection() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_advanced_fields")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Datos del Certificado X.509 (Opcionales)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                imageVector = if (state.isAdvancedSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = state.isAdvancedSectionExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = state.commonName,
                                onValueChange = { viewModel.onCommonNameChange(it) },
                                label = { Text("Nombre y Apellidos / Autor (CN)") },
                                modifier = Modifier.fillMaxWidth().testTag("common_name_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.organization,
                                onValueChange = { viewModel.onOrganizationChange(it) },
                                label = { Text("Organización / Empresa (O)") },
                                modifier = Modifier.fillMaxWidth().testTag("organization_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.organizationalUnit,
                                onValueChange = { viewModel.onOrganizationalUnitChange(it) },
                                label = { Text("Unidad / Departamento (OU)") },
                                modifier = Modifier.fillMaxWidth().testTag("unit_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = state.countryCode,
                                onValueChange = { viewModel.onCountryCodeChange(it) },
                                label = { Text("Código de País (2 letras, ej: ES, MX, US)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Public, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("country_input"),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Botón principal de Generación
            Button(
                onClick = { viewModel.generateKeystore() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("generate_keystore_button"),
                enabled = !state.isGenerating,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Generando certificado y claves...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Generar Keystore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Diálogo de error
        state.errorMessage?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Aviso") },
                text = { Text(errorMsg) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Entendido")
                    }
                }
            )
        }

        // Diálogo de éxito
        state.createdKeystore?.let { created ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSuccessDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        text = "¡Keystore Generado!",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("El archivo se ha creado y guardado localmente de forma segura.")
                        Text(
                            text = "Archivo: ${created.fileName}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Alias: ${created.alias}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Algoritmo: ${created.keyAlgorithm}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = created.id
                            viewModel.dismissSuccessDialog()
                            onNavigateToDetail(id)
                        },
                        modifier = Modifier.testTag("view_created_keystore_button")
                    ) {
                        Text("Ver Detalles")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            KeystoreExportHelper.shareKeystoreFile(context, created)
                        },
                        modifier = Modifier.testTag("share_created_keystore_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartir")
                    }
                }
            )
        }
    }
}
