package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Categorías temáticas para organizar las preguntas frecuentes de la guía.
 */
enum class GuideCategory(val label: String, val icon: ImageVector) {
    ALL("Todas", Icons.Default.MenuBook),
    FORMATS("Formatos (.jks / .p12)", Icons.Default.VpnKey),
    ALGORITHMS("Algoritmos (RSA / ECDSA)", Icons.Default.Security),
    VALIDITY("Validez y Certificados", Icons.Default.Verified),
    SECURITY("Seguridad y Respaldo", Icons.Default.Lightbulb)
}

/**
 * Modelo de datos representativo de una pregunta y respuesta en la guía.
 */
data class GuideItem(
    val id: String,
    val category: GuideCategory,
    val question: String,
    val answerSummary: String,
    val detailedAnswer: List<String>,
    val badgeLabel: String? = null,
    val badgeColor: Color? = null
)

/**
 * Pantalla dedicada de Guía Criptográfica y Preguntas Frecuentes sobre Firma de Aplicaciones Móviles.
 * Diseñada específicamente para uso táctil y lectura cómoda en pantallas de smartphones.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CryptoGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(GuideCategory.ALL) }
    var expandedItemIds by remember { mutableStateOf(setOf<String>("rec_format", "rec_algo")) }

    // Colección completa de preguntas y respuestas técnicas
    val guideItems = remember {
        listOf(
            GuideItem(
                id = "rec_format",
                category = GuideCategory.FORMATS,
                question = "¿Cuál formato de almacén es el más recomendable para la industria móvil?",
                answerSummary = "El estándar oficial recomendado por Google y la industria es PKCS#12 (guardado como .jks o .keystore).",
                detailedAnswer = listOf(
                    "• El formato interno recomendado: PKCS#12 (RFC 7292). A partir de Java 9 y en las versiones modernas de Android, el formato propietario JKS quedó desaconsejado, adoptando PKCS#12 como el estándar oficial de apksigner y Android Studio.",
                    "• Extensión en Android/Flutter: Se recomienda guardar con extensión .jks o .keystore porque el 99% de las plantillas de build.gradle, tutoriales y herramientas de desarrollo esperan encontrar ese nombre de archivo.",
                    "• Extensión .p12: Es exactamente el mismo formato PKCS12, pero es la extensión estándar universal fuera del ecosistema Java (ideal para Fastlane, iOS, macOS, OpenSSL y pipelines de CI/CD multiplataforma)."
                ),
                badgeLabel = "Estándar Oficial",
                badgeColor = Color(0xFF2E7D32)
            ),
            GuideItem(
                id = "rec_algo",
                category = GuideCategory.ALGORITHMS,
                question = "¿Qué algoritmo debo elegir para firmar mis apps: RSA o Curvas Elípticas (ECDSA)?",
                answerSummary = "RSA de 2048 o 4096 bits es el estándar de máxima compatibilidad en Android. ECDSA es moderno y ultraligero.",
                detailedAnswer = listOf(
                    "• RSA (2048 o 4096 bits): Es el rey indiscutible de la compatibilidad universal. Funciona sin excepciones en cualquier versión de Android (desde Android 1.0 hasta Android 16), en cualquier esquema de firma (v1 JAR, v2, v3) y en todas las tiendas de aplicaciones (Google Play, Uptodown, F-Droid, Huawei).",
                    "• Curvas Elípticas ECDSA (P-256, P-384, P-521): Ofrecen firmas matemáticas mucho más pequeñas, menor uso de batería y CPU del procesador móvil y seguridad superior por bit. Sin embargo, requieren Android 7.0+ (API 24) y esquemas APK Signature Scheme v2 o superior.",
                    "• Consejo práctico: Para distribución universal sin fricciones, elige RSA 2048 o 4096 bits. Si tu app tiene minSdk 24+ y buscas eficiencia y modernidad, ECDSA NIST P-256 es una opción formidable."
                ),
                badgeLabel = "Recomendado",
                badgeColor = Color(0xFF1565C0)
            ),
            GuideItem(
                id = "rsa_2048_vs_4096",
                category = GuideCategory.ALGORITHMS,
                question = "¿Es mejor RSA 2048 bits o RSA 4096 bits?",
                answerSummary = "RSA 2048 bits es el balance perfecto entre velocidad y seguridad; 4096 bits ofrece longevidad extrema.",
                detailedAnswer = listOf(
                    "• RSA 2048 bits: Es el valor predeterminado en Android Studio y apksigner. Es completamente seguro contra ataques informáticos actuales y se genera/firma de forma casi instantánea en teléfonos móviles.",
                    "• RSA 4096 bits: Proporciona una resistencia matemática muy superior a largo plazo frente a avances de potencia de cálculo. Sin embargo, el par de claves toma significativamente más tiempo y memoria en generarse en un smartphone y el certificado binario resultante es un poco más grande."
                ),
                badgeLabel = "Comparativa",
                badgeColor = Color(0xFF6A1B9A)
            ),
            GuideItem(
                id = "diff_jks_p12_keystore",
                category = GuideCategory.FORMATS,
                question = "¿Qué diferencia real hay entre un archivo .jks, .keystore y .p12?",
                answerSummary = "En la práctica moderna de Android, los tres almacenan tu par asimétrico y certificado; la diferencia es el contenedor y convención.",
                detailedAnswer = listOf(
                    "• .jks (Java KeyStore): Históricamente usaba un formato binario propietario de Sun/Oracle. En Android moderno, la app y el JDK guardan contenido PKCS12 internamente bajo esta extensión.",
                    "• .keystore: Es una extensión genérica tradicional muy popular en Flutter, Unity, Unreal Engine y scripts clásicos de Android.",
                    "• .p12 / .pfx (PKCS#12): Estándar abierto internacional (RFC 7292). No depende de Java. Es universalmente compatible con servidores web, navegadores, llaveros de iOS/macOS y comandos de OpenSSL.",
                    "• Puedes usar nuestro Conversor JKS ⟷ PKCS12 en la pantalla de detalle de cualquier llave para alternar entre ellos sin alterar tus certificados ni claves."
                )
            ),
            GuideItem(
                id = "convert_impact",
                category = GuideCategory.FORMATS,
                question = "¿Puedo convertir mi .jks a .p12 sin romper las actualizaciones de mi app en tiendas?",
                answerSummary = "Sí, 100%. La identidad de firma reside en el par de claves y el certificado X.509, no en la extensión del archivo.",
                detailedAnswer = listOf(
                    "• Las tiendas de aplicaciones (Google Play, Uptodown, F-Droid) y el sistema Android validan únicamente que las huellas digitales SHA-256 del certificado público coincidan exactamente con la versión anterior de la app instalada.",
                    "• Nuestro conversor preserva idéntico el certificado X.509 y la clave privada. Por lo tanto, un APK firmado con un .p12 generado mediante la conversión de tu .jks original actualizará tu app sin ningún conflicto."
                ),
                badgeLabel = "100% Compatible",
                badgeColor = Color(0xFF2E7D32)
            ),
            GuideItem(
                id = "keystore_validity",
                category = GuideCategory.VALIDITY,
                question = "¿Cuántos años de validez debo asignarle a mi keystore?",
                answerSummary = "Se recomienda un mínimo absoluto de 25 a 30 años (o 100 años para máxima tranquilidad).",
                detailedAnswer = listOf(
                    "• Si la clave de firma de tu aplicación caduca, el sistema Android no permitirá instalar actualizaciones de tu app sobre dispositivos de usuarios existentes.",
                    "• Google Play Console exige explícitamente que la validez del certificado abarque como mínimo hasta el 22 de octubre de 2033 (más de 25 años desde la creación de Android).",
                    "• En nuestra app, el control deslizante te permite fijar desde 1 día (para pruebas rápidas y debug) hasta 100 años con un solo toque."
                ),
                badgeLabel = "Importante",
                badgeColor = Color(0xFFE65100)
            ),
            GuideItem(
                id = "lost_keystore",
                category = GuideCategory.SECURITY,
                question = "¿Qué ocurre si pierdo mi archivo Keystore o mis contraseñas?",
                answerSummary = "No podrás volver a actualizar tu aplicación a menos que uses Google Play App Signing.",
                detailedAnswer = listOf(
                    "• En canales de distribución directa (Uptodown, GitHub Releases, APKs directos): La pérdida del archivo o de las contraseñas significa que nunca más podrás firmar una actualización que reemplace la app existente. Los usuarios tendrían que desinstalar la app vieja (perdiendo sus datos) e instalar una nueva firma.",
                    "• En Google Play Console: Solo si activaste 'Play App Signing' podrás contactar con el soporte de Google para restablecer la clave de subida (Upload Key). Si no usas Play App Signing, la pérdida es irreversible.",
                    "• Recomendación de oro: Exporta el paquete comprimido '.zip All-in-One' desde la app y guárdalo en una memoria física o nube privada segura."
                ),
                badgeLabel = "Crítico",
                badgeColor = Color(0xFFC62828)
            ),
            GuideItem(
                id = "fingerprints_use",
                category = GuideCategory.VALIDITY,
                question = "¿Para qué sirven las huellas digitales SHA-256 y SHA-1 que muestra la app?",
                answerSummary = "Son el documento de identidad criptográfico de tu app frente a APIs de Google, Firebase y tiendas.",
                detailedAnswer = listOf(
                    "• SHA-256: Se utiliza obligatoriamente para configurar enlaces universales (App Links / assetlinks.json), registrar tu app en Google Play Console y configurar SDKs de autenticación modernos.",
                    "• SHA-1: Requerida por Firebase Authentication, Google Sign-In (OAuth) y Google Maps SDK para verificar que las peticiones a la API provengan únicamente de tu binario legítimo.",
                    "• En nuestra app, puedes copiar cualquiera de estas dos huellas en 1 solo toque desde la pantalla de detalle."
                )
            ),
            GuideItem(
                id = "share_certificate",
                category = GuideCategory.SECURITY,
                question = "¿Es seguro compartir los certificados .pem, .crt o .der extraídos por la app?",
                answerSummary = "Sí, es completamente seguro. Contienen únicamente la clave pública y datos del titular.",
                detailedAnswer = listOf(
                    "• Los archivos de certificado público (.pem, .crt, .der) no contienen tu clave privada ni tus contraseñas.",
                    "• Son los archivos exactos que te solicitan plataformas como Google Cloud, Facebook Developers, Huawei AppGallery o el registro de llaves de Google Play para verificar tu titularidad.",
                    "• Lo que NUNCA debes compartir públicamente es tu archivo .jks / .p12 original, su cadena Base64 ni tus contraseñas."
                )
            ),
            GuideItem(
                id = "apk_signature_schemes",
                category = GuideCategory.ALGORITHMS,
                question = "¿Qué significan los esquemas de firma APK v1, v2 y v3?",
                answerSummary = "Son las distintas generaciones de verificación de integridad creadas por Google para Android.",
                detailedAnswer = listOf(
                    "• Esquema v1 (JAR Signature): Firma archivo por archivo dentro del ZIP del APK. Es el más antiguo y lento de verificar, pero funciona en cualquier versión de Android.",
                    "• Esquema v2 (APK Signing Block): Introducido en Android 7.0 (API 24). Protege el binario completo mediante bloques de firma antes de descomprimir el APK, acelerando enormemente la instalación y evitando alteraciones de bytes.",
                    "• Esquema v3: Introducido en Android 9.0 (API 28). Añade compatibilidad para rotación de claves criptográficas en caso de que una llave se vea comprometida.",
                    "• Los keystores generados en nuestra app son 100% compatibles con apksigner en esquemas v1, v2 y v3."
                )
            )
        )
    }

    // Filtrado de elementos según búsqueda y categoría seleccionada
    val filteredItems = remember(searchQuery, selectedCategory, guideItems) {
        guideItems.filter { item ->
            val matchesCategory = selectedCategory == GuideCategory.ALL || item.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                item.question.contains(searchQuery, ignoreCase = true) ||
                item.answerSummary.contains(searchQuery, ignoreCase = true) ||
                item.detailedAnswer.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Guía de Firma y Criptografía",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Estándares móviles, formatos y buenas prácticas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("guide_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar a ajustes"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Tarjeta de bienvenida y propósito pedagógico
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manual Práctico para Desarrolladores",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Respuestas claras y técnicas a las preguntas más habituales sobre firma de APKs, estándares recomendados y seguridad criptográfica sin PC.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Barra de búsqueda rápida para móvil
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guide_search_field"),
                    placeholder = { Text("Buscar tema, duda, .p12, RSA, validez...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Selector horizontal de categorías (Chips)
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GuideCategory.values().forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category.label, style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Conteo de resultados
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PREGUNTAS Y TEMAS (${filteredItems.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (searchQuery.isNotEmpty() || selectedCategory != GuideCategory.ALL) {
                        Text(
                            text = "Filtro activo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No se encontraron coincidencias",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Prueba con otros términos como 'RSA', 'formato', 'validez' o selecciona la categoría 'Todas'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    val isExpanded = expandedItemIds.contains(item.id)
                    val rotationState by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                        label = "expand_rotation"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                expandedItemIds = if (isExpanded) {
                                    expandedItemIds - item.id
                                } else {
                                    expandedItemIds + item.id
                                }
                            }
                            .animateContentSize(animationSpec = tween(250, easing = FastOutSlowInEasing))
                            .testTag("guide_item_${item.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isExpanded) 3.dp else 1.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cabecera: Badges y Pregunta
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (item.badgeLabel != null && item.badgeColor != null) {
                                        Surface(
                                            color = item.badgeColor.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            Text(
                                                text = item.badgeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = item.badgeColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.question,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        expandedItemIds = if (isExpanded) {
                                            expandedItemIds - item.id
                                        } else {
                                            expandedItemIds + item.id
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                                        modifier = Modifier.rotate(rotationState),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Resumen directo de la respuesta
                            Text(
                                text = item.answerSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isExpanded) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (isExpanded) FontWeight.Medium else FontWeight.Normal
                            )

                            // Contenido detallado colapsable
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )

                                    item.detailedAnswer.forEach { bullet ->
                                        Text(
                                            text = bullet,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.25f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
