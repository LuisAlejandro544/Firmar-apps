package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

/**
 * Paleta de colores predefinidos con tonalidades estéticas optimizadas para contraste.
 */
data class PresetColor(val name: String, val color: Color)

private val PRESET_COLORS = listOf(
    PresetColor("Índigo", Color(0xFF3B5DF6)),
    PresetColor("Cobalto", Color(0xFF1D4ED8)),
    PresetColor("Océano", Color(0xFF0284C7)),
    PresetColor("Cian", Color(0xFF06B6D4)),
    PresetColor("Esmeralda", Color(0xFF10B981)),
    PresetColor("Menta", Color(0xFF059669)),
    PresetColor("Naranja", Color(0xFFF97316)),
    PresetColor("Ámbar", Color(0xFFF59E0B)),
    PresetColor("Carmesí", Color(0xFFEF4444)),
    PresetColor("Fucsia", Color(0xFFEC4899)),
    PresetColor("Púrpura", Color(0xFF8B5CF6)),
    PresetColor("Grafito", Color(0xFF475569))
)

/**
 * Pantalla dedicada a la personalización completa del color y apariencia:
 * - Selección de Modo Claro, Modo Oscuro o Seguir el Sistema.
 * - Soporte para Material You (colores dinámicos en Android 12+).
 * - Selector de paletas predefinidas.
 * - Deslizadores RGB interactivos para personalización al 100%.
 * - Previsualización de componentes en tiempo real.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColorPickerScreen(
    themeViewModel: ThemeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeSettings by themeViewModel.themeSettings.collectAsStateWithLifecycle()
    val isDynamicSupported = themeViewModel.isMaterialYouSupported

    // Estados locales para los deslizadores RGB basados en el color actual
    val currentColor = themeSettings.customPrimaryColor
    var redValue by remember(currentColor) { mutableFloatStateOf(currentColor.red * 255f) }
    var greenValue by remember(currentColor) { mutableFloatStateOf(currentColor.green * 255f) }
    var blueValue by remember(currentColor) { mutableFloatStateOf(currentColor.blue * 255f) }

    val hexCode = remember(redValue, greenValue, blueValue) {
        String.format(
            "#%02X%02X%02X",
            redValue.roundToInt(),
            greenValue.roundToInt(),
            blueValue.roundToInt()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tema y Colores",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("color_picker_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a Configuración"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. SELECTOR DE MODO DE APARIENCIA (Sistema, Claro, Oscuro)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "MODO DE APARIENCIA",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeModeCard(
                        title = "Sistema",
                        icon = Icons.Default.BrightnessAuto,
                        selected = themeSettings.themeMode == AppThemeMode.SYSTEM,
                        onClick = { themeViewModel.setThemeMode(AppThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeModeCard(
                        title = "Claro",
                        icon = Icons.Default.LightMode,
                        selected = themeSettings.themeMode == AppThemeMode.LIGHT,
                        onClick = { themeViewModel.setThemeMode(AppThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeModeCard(
                        title = "Oscuro",
                        icon = Icons.Default.DarkMode,
                        selected = themeSettings.themeMode == AppThemeMode.DARK,
                        onClick = { themeViewModel.setThemeMode(AppThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. OPCIÓN MATERIAL YOU (Color Dinámico en Android 12+)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (themeSettings.useDynamicColor)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (themeSettings.useDynamicColor) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (themeSettings.useDynamicColor) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column {
                            Text(
                                text = "Material You (Color Dinámico)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDynamicSupported) {
                                    "Extrae automáticamente los colores del fondo de pantalla de tu dispositivo."
                                } else {
                                    "Requiere Android 12 o posterior (no disponible en este dispositivo)."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = themeSettings.useDynamicColor && isDynamicSupported,
                        onCheckedChange = { isChecked ->
                            if (isDynamicSupported) {
                                themeViewModel.setUseDynamicColor(isChecked)
                            }
                        },
                        enabled = isDynamicSupported,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("material_you_switch")
                    )
                }
            }

            // 3. PALETAS Y PERSONALIZADOR DE COLOR (Visible si no se usa Material You)
            if (!themeSettings.useDynamicColor) {
                // Paletas predefinidas
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PALETAS RECOMENDADAS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PRESET_COLORS.forEach { preset ->
                            val isSelected = themeSettings.customPrimaryColor.toArgb() == preset.color.toArgb()

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    themeViewModel.setCustomPrimaryColor(preset.color)
                                },
                                label = { Text(preset.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(preset.color)
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

                // Selector 100% Personalizado (Sliders RGB)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Color 100% Personalizable",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ajusta los canales RGB libremente",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Muestra circular grande del color seleccionado con su código HEX
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = hexCode,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(redValue / 255f, greenValue / 255f, blueValue / 255f))
                                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                            }
                        }

                        // Slider Rojo
                        RgbSliderRow(
                            label = "Rojo (R)",
                            value = redValue,
                            color = Color(0xFFEF4444),
                            onValueChange = {
                                redValue = it
                                val newColor = Color(redValue / 255f, greenValue / 255f, blueValue / 255f)
                                themeViewModel.setCustomPrimaryColor(newColor)
                            }
                        )

                        // Slider Verde
                        RgbSliderRow(
                            label = "Verde (G)",
                            value = greenValue,
                            color = Color(0xFF10B981),
                            onValueChange = {
                                greenValue = it
                                val newColor = Color(redValue / 255f, greenValue / 255f, blueValue / 255f)
                                themeViewModel.setCustomPrimaryColor(newColor)
                            }
                        )

                        // Slider Azul
                        RgbSliderRow(
                            label = "Azul (B)",
                            value = blueValue,
                            color = Color(0xFF3B82F6),
                            onValueChange = {
                                blueValue = it
                                val newColor = Color(redValue / 255f, greenValue / 255f, blueValue / 255f)
                                themeViewModel.setCustomPrimaryColor(newColor)
                            }
                        )

                        // Botón para restablecer al color predeterminado
                        OutlinedButton(
                            onClick = { themeViewModel.resetToDefaultColor() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_color_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restablecer al Azul Índigo Original")
                        }
                    }
                }
            }

            // 4. VISTA PREVIA EN VIVO DE COMPONENTES
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "VISTA PREVIA EN TIEMPO REAL",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Así lucen los elementos con tu estilo:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Botón Acción")
                            }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tarjeta Tonal",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Fila interactiva para cada canal de color RGB con etiqueta, valor numérico y slider.
 */
@Composable
private fun RgbSliderRow(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Text(
                text = value.roundToInt().toString(),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
    }
}

/**
 * Tarjeta seleccionable para el modo de tema (Sistema, Claro, Oscuro).
 */
@Composable
private fun ThemeModeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
