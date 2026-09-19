package com.example.ui.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Pantalla del Inspector Forense de Certificados X.509 y Verificador de Firma Real.
 * Permite auditar en detalle la estructura ASN.1, huellas digitales y comprobar
 * matemáticamente la autenticidad de la firma asimétrica en vivo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CertificateForensicScreen(
    viewModel: CertificateForensicViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableKeystores by viewModel.availableKeystores.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encabezado descriptivo de la herramienta
            ForensicHeaderCard()

            // Selector de Keystores disponibles en el almacenamiento de la app
            KeystoreSelectorSection(
                keystores = availableKeystores,
                currentSelectedTitle = (uiState as? ForensicUiState.Success)?.result?.keystoreTitle,
                onSelect = { viewModel.inspectKeystore(it) },
                onGenerateEphemeral = { viewModel.inspectEphemeralTestKeystore() }
            )

            // Contenido según el estado
            when (val state = uiState) {
                is ForensicUiState.Idle -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Selecciona una keystore de la app o presiona el botón inferior para realizar una prueba forense en vivo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.inspectEphemeralTestKeystore() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auditar Clave de Prueba en Vivo")
                            }
                        }
                    }
                }
                is ForensicUiState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Desempaquetando estructura ASN.1 y verificando firma...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is ForensicUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is ForensicUiState.Success -> {
                    val res = state.result

                    // 1. Tarjeta de Verificación Criptográfica Real (Sign & Verify)
                    RealSignatureVerificationCard(
                        result = res,
                        onCopy = { label, text ->
                            copyToClipboard(context, label, text)
                            scope.launch { snackbarHostState.showSnackbar("$label copiado al portapapeles") }
                        }
                    )

                    // 2. Estructura ASN.1 y Metadatos de la Clave
                    Asn1StructureCard(
                        result = res,
                        onCopy = { label, text ->
                            copyToClipboard(context, label, text)
                            scope.launch { snackbarHostState.showSnackbar("$label copiado") }
                        }
                    )

                    // 3. Huellas Forenses (SHA-256, SHA-1, MD5)
                    FingerprintsCard(
                        result = res,
                        onCopy = { label, text ->
                            copyToClipboard(context, label, text)
                            scope.launch { snackbarHostState.showSnackbar("$label copiada") }
                        }
                    )

                    // 4. Distinguished Name (DN) X.500
                    DistinguishedNameCard(result = res)

                    // 5. Detalles del archivo físico en disco
                    PhysicalFileCard(result = res)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ForensicHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Inspector Forense de Certificados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Auditoría ASN.1 & Verificación Matemática Real",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Esta herramienta de depuración analiza la estructura interna del certificado X.509, extrae sus huellas unívocas y ejecuta una prueba de firma digital asimétrica en vivo para comprobar con rigor matemático que la clave privada y pública son auténticas y no simuladas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun KeystoreSelectorSection(
    keystores: List<com.example.data.model.KeystoreEntity>,
    currentSelectedTitle: String?,
    onSelect: (com.example.data.model.KeystoreEntity) -> Unit,
    onGenerateEphemeral: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Almacén a Inspeccionar:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (keystores.isEmpty()) {
                Text(
                    text = "Aún no tienes keystores registradas en la app principal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    keystores.forEach { item ->
                        val isSelected = item.title == currentSelectedTitle
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelect(item) },
                            label = { Text(item.title) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
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
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = onGenerateEphemeral,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generar y Auditar Clave Temporal (Test)")
            }
        }
    }
}

@Composable
private fun RealSignatureVerificationCard(
    result: ForensicInspectionResult,
    onCopy: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isSignatureValid) {
                Color(0xFF064E3B).copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (result.isSignatureValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (result.isSignatureValid) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Verificación Criptográfica Real",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (result.isSignatureValid) Color(0xFF047857) else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (result.isSignatureValid) "Firma Digital 100% Auténtica (Sin Simulación)" else "Error en Firma",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (result.isSignatureValid) Color(0xFF059669) else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Se firmó un payload de bytes dinámico en memoria con la clave privada ('${result.signatureAlgorithm}') y se validó inmediatamente con la clave pública del certificado X.509. El resultado matemático fue estrictamente verificado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    DetailRow(label = "Algoritmo de Firma", value = result.signatureAlgorithm)
                    DetailRow(label = "OID Oficial ASN.1", value = result.signatureAlgorithmOid)
                    DetailRow(label = "Tamaño de Firma", value = "${result.signatureSizeBytes} bytes (${result.signatureSizeBytes * 8} bits)")
                    DetailRow(label = "Payload de Prueba", value = result.testPayload)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Muestra Hexadecimal de la Firma:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = result.signatureSampleHex,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Asn1StructureCard(
    result: ForensicInspectionResult,
    onCopy: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Estructura ASN.1 & Clave Pública",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(label = "Algoritmo de Clave", value = result.keyAlgorithm)
            DetailRow(label = "Tamaño del Módulo RSA", value = "${result.keySizeBits} bits")
            DetailRow(label = "Exponente Público (e)", value = result.publicExponent)
            DetailRow(label = "Número de Serie (Hex)", value = result.serialNumberHex)
            DetailRow(label = "Número de Serie (Dec)", value = result.serialNumberDec)
            DetailRow(label = "Válido Desde", value = result.validFromFormatted)
            DetailRow(label = "Válido Hasta", value = result.validToFormatted)
            DetailRow(label = "Estado de Vigencia", value = result.validityRemainingText)
        }
    }
}

@Composable
private fun FingerprintsCard(
    result: ForensicInspectionResult,
    onCopy: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Huellas Forenses Digitales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hashes unívocos calculados byte a byte sobre el certificado X.509 codificado en DER.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            FingerprintItem(label = "SHA-256", value = result.sha256Fingerprint, onCopy = onCopy)
            Spacer(modifier = Modifier.height(8.dp))
            FingerprintItem(label = "SHA-1", value = result.sha1Fingerprint, onCopy = onCopy)
            Spacer(modifier = Modifier.height(8.dp))
            FingerprintItem(label = "MD5", value = result.md5Fingerprint, onCopy = onCopy)
        }
    }
}

@Composable
private fun FingerprintItem(
    label: String,
    value: String,
    onCopy: (String, String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = { onCopy(label, value) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar $label",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DistinguishedNameCard(result: ForensicInspectionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Distinguished Name (DN X.500)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            result.rdnFields.forEach { (k, v) ->
                val descriptiveLabel = when (k.uppercase()) {
                    "CN" -> "Nombre Común (CN)"
                    "OU" -> "Unidad Organizacional (OU)"
                    "O" -> "Organización (O)"
                    "C" -> "Código de País (C)"
                    "L" -> "Localidad / Ciudad (L)"
                    "ST" -> "Estado / Provincia (ST)"
                    else -> k
                }
                DetailRow(label = descriptiveLabel, value = v)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sujeto Completo:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = result.subjectDn,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PhysicalFileCard(result: ForensicInspectionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Almacén Físico en Disco",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(label = "Nombre de Archivo", value = result.fileName)
            DetailRow(label = "Formato de Contenedor", value = "PKCS#12 (Estándar Java / Android)")
            DetailRow(label = "Tamaño Físico", value = "${result.fileSizeBytes} bytes (~${result.fileSizeBytes / 1024} KB)")
            DetailRow(label = "Alias Registrado", value = result.alias)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
