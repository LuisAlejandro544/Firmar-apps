# Roadmap del Proyecto: Keystore Creator 🗺️

Este documento define el plan de evolución y las próximas funcionalidades de la aplicación.

---

## 🎯 Próximo Paso Prioritario (Siguiente Implementación)

### 📌 Firma Móvil de APKs y Zipalign Integrado (Fase 7)
* **Objetivo:** Permitir al usuario seleccionar un APK o AAB generado en su teléfono y firmarlo directamente utilizando cualquiera de las keystores guardadas en la biblioteca local mediante APK Signature Scheme v1, v2 y v3 con alineación `zipalign`.
* **Casos de uso clave:**
  - **Firma autónoma sin PC:** Firmar APKs de depuración o producción directamente desde la memoria del teléfono.
  - **Distribución en tiendas alternativas:** Generar binarios listos para Uptodown, GitHub Releases o F-Droid sin necesidad de terminales o emuladores de escritorio.

---

## 📅 Estado de las Fases

### ✅ Fase 1: Fundamentos y Validación (Completada)
- [x] Motor criptográfico para generación de claves RSA (2048 y 4096 bits).
- [x] Construcción y firma de certificados **X.509 v3** (RFC 5280) autofirmados con extensiones canónicas:
  - [x] `BasicConstraints(false)` marcada crítica (entidad final, no CA).
  - [x] `KeyUsage(digitalSignature)` marcada crítica (firma digital de paquetes Android).
  - [x] `SubjectKeyIdentifier` y `AuthorityKeyIdentifier` (hashes SHA-1 del par de claves para compatibilidad total con apksigner v2/v3).
- [x] Soporte dual de formatos de salida: `.jks` (estándar Android) y `.keystore` (clásico y Flutter) con auto-formato inteligente.
- [x] Rango de validez granular desde 1 día hasta 100 años con Slider continuo y accesos rápidos.
- [x] Empaquetado en formato estándar PKCS12 compatible con `.jks` y `.keystore`.
- [x] Base de datos local Room para persistencia offline segura de credenciales.
- [x] Navegación cómoda con `NavigationBar` y pantalla de detalle individual.
- [x] Extracción automática de huellas digitales SHA-256 y SHA-1.
- [x] Generador de fragmento de código de firma para `build.gradle.kts`.
- [x] Exportación y compartición de archivos vía Android `FileProvider`.

---

### ✅ Fase 2: Conversión a Base64, CI/CD y Seguridad de Portapapeles (Completada)
- [x] Generación de Base64 con 1 clic en la pantalla de detalle (`KeystoreDetailScreen`).
- [x] Visor de texto Base64 con contador de caracteres, tamaño estimado y botón rápido de copia.
- [x] Compartición de la cadena como texto o exportación directa de archivo físico `.base64` vía FileProvider.
- [x] **Generador de Workflow Completo para GitHub Actions (`.github/workflows/build-and-sign.yml`):** Pipeline preconfigurado con el nombre de archivo, alias, variables de entorno, decodificación Base64, compilación con Gradle y apksigner.
- [x] Generador de fragmento de decodificación individual para workflows de GitHub Actions CI/CD.
- [x] Notificaciones in-app exclusivas de la aplicación con diseño M3 para copiado de credenciales sensibles.
- [x] Supresión de la notificación nativa tosca de Android (`EXTRA_IS_SENSITIVE`) para proteger contraseñas.
- [x] Advertencia explícita sobre riesgos de acceso al portapapeles por aplicaciones de terceros.
- [x] **Eliminación automática del portapapeles a los 2 minutos:** Programación de limpieza que verifica estrictamente que el portapapeles aún contenga el dato copiado desde nuestra app antes de vaciarlo, protegiendo contra interferencias con datos copiados de otras aplicaciones.

---

### ✅ Fase 3: Personalización de Temas, Colores y Ajustes (Completada)
- [x] Nuevo menú de Configuración (`SettingsScreen`) accesible desde la barra de navegación principal.
- [x] Pantalla dedicada para personalización de colores y tema (`ColorPickerScreen`).
- [x] Selector triple de modo de apariencia: Seguir el sistema, Modo claro y Modo oscuro.
- [x] Soporte nativo para Material You (colores dinámicos) en dispositivos Android 12+ (API 31+).
- [x] Selector de paletas predefinidas de colores armónicos (Índigo, Cobalto, Océano, Cian, Esmeralda, Menta, Naranja, Ámbar, Carmesí, Fucsia, Púrpura, Grafito).
- [x] Selector de color 100% personalizable mediante deslizadores de canales RGB y visualizador hexadecimal.
- [x] Vista previa de componentes en vivo que responde en tiempo real a los cambios visuales.
- [x] Persistencia automática con `ThemePreferences` y `ThemeViewModel`.

---

### ✅ Fase 4: Herramientas de Depuración y Auditoría (Crypto Lab) (Completada)
- [x] Actividad independiente en Android (`DebugToolsActivity`) con propio icono lanzador ("Crypto Lab (Debug)") en el cajón de aplicaciones.
- [x] Acceso directo bidireccional desde la app principal (menú de Ajustes) y navegación fluida entre ambas.
- [x] **Inspector Forense de Certificados X.509:**
  - [x] Verificación matemática real de firma digital con `SHA256withRSA` (par de claves probado en vivo, descartando simulaciones).
  - [x] Extracción y desglose de estructura ASN.1: número de serie hex/dec, tamaño de módulo RSA (2048/4096), exponente público (`65537`), OID oficial de algoritmo (`1.2.840.113549.1.1.11`).
  - [x] Cálculo byte a byte de huellas forenses **SHA-256**, **SHA-1** y **MD5** con copiado en 1 toque.
  - [x] Auditoría de validez temporal y análisis de campos RDN (`CN`, `OU`, `O`, `C`) del sujeto X.500.
  - [x] Generador de almacenes efímeros de prueba para auditorías en caliente.
- [x] **Benchmark de Rendimiento Criptográfico (32-bit vs 64-bit):**
  - [x] Telemetría en vivo del procesador móvil, núcleos de CPU, RAM de JVM y detección del modo nativo de arquitectura (32 bits vs 64 bits).
  - [x] Medición por fases de milisegundos reales: Entropía segura, Par de claves RSA, Certificado X.509 v3, Serialización PKCS12 y Firma digital.
  - [x] Comparativa de desempeño y ratio multiplicador entre RSA 2048 bits vs RSA 4096 bits.
  - [x] Diagnóstico de hardware y optimización para arquitecturas `armeabi-v7a` y `arm64-v8a`.

---

### ✅ Fase 5: Pipeline CI/CD Automatizado en GitHub Actions (Completada)
- [x] **Workflow de Compilación Debug (`.github/workflows/build-debug-apk.yml`):**
  - [x] Descarga completa del repositorio en runner `ubuntu-latest`.
  - [x] Compilación limpia y sin caché en Java 17 y Gradle (`cache-disabled: true`, `--no-daemon`, `--no-build-cache`).
  - [x] Generación forzada de firma `debug.keystore` desde cero dentro del action sin depender de claves preexistentes.
  - [x] Disparadores por `push`, `pull_request` y manual vía `workflow_dispatch`.
- [x] **Script de Forzado de Firma (`scripts/generate-debug-keystore.sh`):**
  - [x] Destrucción de cualquier firma residual en el entorno antes de compilar.
  - [x] Generación autónoma con `keytool` en formato PKCS12/JKS con par RSA 2048 y validez de 10.000 días.
  - [x] Comprobación de integridad y volcado forense de huellas digitales en consola.
- [x] **Distribución de Artefactos Individuales con Splits Reales de ABI (`splits.abi`):**
  - [x] Configuración de `splits.abi` en Gradle para generar binarios independientes por arquitectura.
  - [x] `APK-Debug-arm64-v8a`: Binario exclusivo ARM de 64 bits con únicamente librerías `lib/arm64-v8a/` (sin residuos de 32 bits ni x86).
  - [x] `APK-Debug-armeabi-v7a`: Binario exclusivo ARM de 32 bits con únicamente librerías `lib/armeabi-v7a/`.
  - [x] `APK-Debug-x86_64`: Binario exclusivo para emuladores con únicamente librerías `lib/x86_64/`.
  - [x] `APK-Debug-Universal`: Binario unificado compatible con todas las arquitecturas.
  - [x] Verificación automatizada con `unzip -l` en los logs del pipeline para auditar las carpetas `lib/` de cada binario.

---

### ✅ Fase 5.1: Refinamiento de Diseño, Tipografía Fija e Insets (Completada)
- [x] **Fijación de Escala de Fuente (`fontScale = 1.0f`):** Desacoplamiento global del tamaño de fuente respecto a las opciones de accesibilidad del teléfono en `MyApplicationTheme`, evitando roturas de layout o desbordes en tarjetas.
- [x] **Corrección de Insets en Pantallas Secundarias:** Eliminación del doble padding superior en `KeystoreDetailScreen` y `ColorPickerScreen`, garantizando un acoplamiento perfecto de la `TopAppBar` a la barra de estado.

---

### ✅ Fase 5.2: Generador de Contraseñas Ultra Seguras y Cifrado Fuerte de Credenciales (Completada)
- [x] **Generador Móvil y Auditor de Contraseñas (`PasswordSecurityEngine` + `zxcvbn`):**
  - [x] Motor de entropía criptográfica basado en `SecureRandom` con exclusión de caracteres confusos para CLI/Gradle.
  - [x] 3 Opciones de longitud configurables: 16 caracteres (Alta), 24 caracteres (Muy Alta) y 32 caracteres (Ultra Segura).
  - [x] Integración de la librería estándar `zxcvbn` para asegurar que las contraseñas generadas alcancen el score máximo 4/4 (indescifrables).
  - [x] Detección activa en vivo al escribir contraseñas manuales con avisos nativos in-app (no Toasts) informando si es descifrable y el tiempo estimado de ataque, sin bloquear la creación de la firma.
  - [x] Botón directo de acceso ergonómico con varita mágica al lado de los campos de contraseña en `GeneratorScreen`.
  - [x] Diálogo modal interactivo con selección de longitud, regeneración instantánea y vista previa monoespaciada.
  - [x] Medidor reactivo de fortaleza de contraseña con barra de progreso cromática según la entropía calculada.
- [x] **Cifrado Fuerte de Credenciales en Reposo (`SecureCredentialsCipher`):**
  - [x] Implementación de cifrado autenticado AES-256-GCM con IV aleatorio de 12 bytes y etiqueta de autenticación de 128 bits.
  - [x] Resguardo de clave maestra en hardware seguro mediante **Android KeyStore Provider** (TEE / StrongBox).
  - [x] Integración transparente en `KeystoreRepository`: contraseñas protegidas al guardar en SQLite (Room) y descifradas en memoria al recuperar.
  - [x] Compatibilidad total hacia atrás con contraseñas en texto plano preexistentes.
  - [x] Suite de pruebas unitarias automatizadas (`CryptoSecurityUnitTest`) para verificar generación, entropía, auditoría zxcvbn y cifrado/descifrado.

---

### ✅ Fase 5.3: Transiciones de Navegación y Micro-animaciones Nativas (Completada)
- [x] **Transiciones Direccionales en Barra Inferior (`MainActivity.kt` + `NavRoutes.kt`):** Deslizamiento horizontal y fundido calculado según el índice de pestañas (Generador=0, Almacén=1, Ajustes=2) con aceleración `FastOutSlowInEasing`.
- [x] **Navegación en Profundidad:** Desplazamiento lateral natural para pantallas de detalle (`KeystoreDetailScreen`) y personalización (`ColorPickerScreen`).
- [x] **Micro-animaciones en Tarjetas (`animateContentSize`):** Transiciones de tamaño fluidas al expandir secciones de formulario, alternar visibilidad de contraseñas, generar claves y alternar opciones de Material You.
- [x] **Navegación por Pestañas en Códigos de Integración (`KeystoreDetailScreen.kt`):** Sustitución del apilamiento vertical largo por un selector de pestañas (`SecondaryTabRow`) con animación de transición horizontal (`AnimatedContent`) entre `build.gradle.kts` y `GitHub Actions CI/CD`, evitando scrolls innecesarios en pantallas táctiles de teléfonos móviles.

---

### ✅ Fase 5.4: Exportación de Certificados Públicos X.509 (.pem / .crt / .der) (Completada)
- [x] **Aislamiento Criptográfico y Cero Exposición de Claves (`CertificateExportHelper`):** Extracción del certificado X.509 v3 desde el almacén PKCS12 / JKS, omitiendo por diseño toda clave privada o contraseña. El archivo resultante es 100% seguro para compartir en Google Cloud, Play Console (App Signing), Firebase y Facebook Developers.
- [x] **Soporte Completo de 3 Formatos Criptográficos:**
  - [x] **PEM (`.pem`):** Codificación Base64 estándar delimitada (`-----BEGIN CERTIFICATE-----` / `-----END CERTIFICATE-----`) con partición exacta a 64 columnas.
  - [x] **CRT (`.crt`):** Formato binario estándar para servidores web y sistemas operativos.
  - [x] **DER (`.der`):** Formato binario nativo ASN.1 sin procesar.
- [x] **Interfaz Móvil Dedicada en Detalle (`KeystoreDetailScreen`):**
  - [x] Tarjeta M3 con banner explicativo de seguridad contra filtraciones.
  - [x] Selector táctil de formatos mediante pestañas animadas (`SecondaryTabRow`).
  - [x] Visor de texto PEM monoespaciado en pantalla con botón de copiado en 1 clic y auto-limpieza del portapapeles.
  - [x] Compartición directa del archivo mediante `FileProvider` a apps externas (Drive, Gmail, Telegram).
  - [x] Guardado directo en el almacenamiento del dispositivo utilizando el selector de documentos nativo de Android (Storage Access Framework `CreateDocument`).
- [x] **Pruebas Unitarias Automatizadas:** Verificación de extracción, formateo PEM/DER/CRT y validación con `CertificateFactory` nativo en `CryptoSecurityUnitTest`.

---

### ✅ Fase 5.5: Paquete All-in-One Ultra-Comprimido (.zip) y Exportación Granular SAF (Completada)
- [x] **Motor de Compresión Nativo Máximo (`StorageCompressionHelper`):**
  - [x] Algoritmo `Deflater(Deflater.BEST_COMPRESSION)` nivel 9 sin dependencias externas pesadas.
  - [x] Compresión de cadenas de texto y metadatos con `GZIPOutputStream`.
  - [x] Empaquetador multi-archivo `ZipOutputStream` con búferes de 8 KB para evitar saturación de memoria en dispositivos de gama media/baja.
  - [x] Telemetría de optimización en disco: cálculo byte a byte y porcentaje de ahorro visualizado en la pantalla de detalle.
- [x] **Paquete Completo All-in-One (.zip):**
  - [x] Empaquetado automático en 1 solo archivo que incluye: almacén (`.jks`/`.keystore`), certificados (`.pem`, `.crt`), Base64 (`.base64`), pipeline CI/CD (`build-and-sign.yml`), configuración Gradle (`signingConfigs.gradle.kts`) y reporte informativo (`info.txt`).
  - [x] Exportación directa a cualquier carpeta del dispositivo vía Storage Access Framework (`CreateDocument`).
  - [x] Compartición directa mediante `FileProvider` a mensajería, nube o administradores de archivos móviles (MT Manager, Drive, Telegram).
- [x] **Exportación Individual con Storage Access Framework (SAF):**
  - [x] Guardado individual del almacén original (`.jks` / `.keystore`) en la ubicación que el usuario elija.
  - [x] Guardado individual de la cadena codificada (`.base64`).
  - [x] Guardado individual del fragmento Gradle (`signingConfigs.gradle.kts`).
  - [x] Guardado individual del pipeline CI/CD (`build-and-sign.yml`).
  - [x] Guardado individual de certificados X.509 (`.pem`, `.crt`, `.der`).

---

### ✅ Fase 6: Importador y Restaurador de Paquetes ZIP Completos (Completada)
- [x] **Motor Autónomo de Importación (`ZipImportHelper`):**
  - [x] Extracción segura en sandbox temporal (`cacheDir/temp_zip_imports/`) con prevención de la vulnerabilidad Zip Slip (validación estricta de rutas canónicas).
  - [x] Detección inteligente de archivos `.jks` / `.keystore`, certificados X.509 (`.pem`/`.crt`), cadenas Base64 (`.base64`), configuración de Gradle (`signingConfigs.gradle.kts`) y workflows CI/CD.
  - [x] Reconstrucción en caliente de keystores a partir de archivos `.base64` en paquetes ZIP que no incluyan el binario directo.
  - [x] Parser heurístico de credenciales: extracción automática de `storePassword`, `keyPassword` y `keyAlias` desde archivos `signingConfigs.gradle.kts` o `INFO_KEYSTORE.txt` preexistentes en el ZIP para evitar tecleo manual en móvil.
  - [x] Validación criptográfica estricta con `java.security.KeyStore` (formato PKCS12 con fallback automático a JKS clásico) probando ambas contraseñas contra la clave privada.
  - [x] Auditoría forense instantánea del certificado X.509: extracción de titular (CN, O, OU, C), fechas de validez y cálculo de huellas digitales SHA-256 y SHA-1.
  - [x] Prevención de colisiones en disco con renombrado automático no destructivo (`mi_llave_imported.jks`).
  - [x] Persistencia reactiva en Room con cifrado fuerte de contraseñas mediante **AES-256-GCM** y clave en hardware TEE/StrongBox.
- [x] **Arquitectura y Capa de Presentación (`ZipImportViewModel` + `ZipImportScreen`):**
  - [x] Interfaz Material Design 3 con tarjetas informativas y badges de estado para cada artefacto detectado dentro del archivo comprimido.
  - [x] Selector nativo de documentos Storage Access Framework (`ActivityResultContracts.OpenDocument()`) sin requerir permisos peligrosos de almacenamiento.
  - [x] Formulario editable de credenciales con visores de contraseña y autocompletado si los datos fueron extraídos del ZIP.
  - [x] Panel de verificación con huellas digitales SHA-256 y botón táctil de confirmación "Validar e Importar".
- [x] **Puntos de Entrada y Navegación:**
  - [x] Botón directo de restauración con icono de ZIP en la barra de búsqueda de la lista de almacenes (`KeystoreListScreen`).
  - [x] Botón de acción en el estado vacío cuando la biblioteca aún no tiene almacenes creados.
  - [x] Registro en `NavRoutes.kt` y `MainActivity.kt` con animación de entrada y gestión limpia de insets edge-to-edge.
- [x] **Pruebas Unitarias Automatizadas:** Pruebas en `CryptoSecurityUnitTest` validando descompresión, inspección de metadatos y rechazo estricto ante contraseñas incorrectas.

---

### ✅ Fase 6.1: Curvas Elípticas (ECDSA), Formato Universal .p12 y Conversor JKS ⟷ PKCS12 (Completada)
- [x] **Soporte de Algoritmos Asimétricos Modernos de Curvas Elípticas (ECDSA):**
  - [x] Generación de pares de claves mediante `ECGenParameterSpec` en curvas estándar NIST:
    - [x] `secp256r1 (NIST P-256)`: Equivalente a RSA 3072 con una fracción minúscula de peso computacional.
    - [x] `secp384r1 (NIST P-384)`: Nivel de seguridad TOP-SECRET militar.
    - [x] `secp521r1 (NIST P-521)`: Máxima resistencia criptográfica teórica de curvas de Weierstrass.
  - [x] Algoritmos de firma digital adaptativos: `SHA256withECDSA`, `SHA384withECDSA` y `SHA512withECDSA`.
  - [x] Certificados X.509 v3 con clave pública EC y OIDs correspondientes.
  - [x] Selector ergonómico en formulario (`GeneratorScreen`): elección entre RSA Clásico (2048/4096) y Curva Elíptica (P-256, P-384, P-521).
- [x] **Soporte de Formato Universal `.p12` (PKCS#12 Universal):**
  - [x] Selector de extensión triple: `.jks` (Android Estándar), `.keystore` (Clásico/Flutter) y `.p12` (Estándar Universal de la Industria).
  - [x] Manejo directo en `KeystoreGenerator` con provider Bouncy Castle para serialización binaria nativa PKCS12.
- [x] **Conversor Bidireccional de Formatos Criptográficos (`KeystoreFormatConverter`):**
  - [x] Conversión exacta entre `JKS` y `PKCS12 (.p12 / .keystore)` sin alterar la clave privada ni el certificado X.509.
  - [x] Verificación de huellas SHA-256 y SHA-1 idénticas tras la conversión para garantizar que el APK firmado mantenga su identidad ante Google Play o tiendas de apps.
  - [x] Opción de conservar las contraseñas originales o asignar contraseñas nuevas a la llave convertida.
  - [x] Personalización del nombre de archivo resultante (`mi_llave_converted.p12`).
  - [x] Registro automático de la nueva llave en Room con cifrado AES-256-GCM.
- [x] **Interfaz de Conversión en Detalle (`KeystoreDetailScreen`):**
  - [x] Detección visual del formato actual (.jks, .p12, .keystore) e indicador/badge de Curva Elíptica si aplica.
  - [x] Tarjeta de conversión dedicada con botón de acción.
  - [x] Diálogo modal interactivo con selección de formato de destino mediante FilterChips, switch de contraseñas y validaciones.
  - [x] Redirección fluida a la nueva keystore convertida al finalizar.

---

### ✅ Fase 6.2: Guía de Firma y Criptografía Móvil (Completada)
- [x] **Pantalla Dedicada de Guía Técnica (`CryptoGuideScreen`):**
  - [x] Buscador en tiempo real de dudas técnicas con teclado en pantalla.
  - [x] Filtros por categorías temáticas con chips táctiles: Formatos, Algoritmos, Validez y Seguridad.
  - [x] Tarjetas interactivas colapsables (acordeón animado con `animateContentSize` y flechas rotativas).
  - [x] Badges temáticos ("Estándar Oficial", "Recomendado", "Comparativa", "Importante", "Crítico").
  - [x] Respuestas técnicas y didácticas sobre:
    - [x] Formato más recomendable en la industria móvil (PKCS#12, desuso de JKS, convenciones .jks vs .p12).
    - [x] Elección de algoritmo: RSA 2048/4096 (compatibilidad universal) vs ECDSA (curvas elípticas ligeras).
    - [x] RSA 2048 vs RSA 4096 bits.
    - [x] Diferencias reales entre `.jks`, `.keystore` y `.p12`.
    - [x] Compatibilidad total al convertir formatos sin romper actualizaciones de apps.
    - [x] Validez de la keystore (25 a 100 años) y consecuencias de caducidad.
    - [x] Qué ocurre si se pierde la firma o las contraseñas.
    - [x] Utilidad de las huellas digitales SHA-256 y SHA-1 en Firebase y Google Play.
    - [x] Seguridad al compartir certificados públicos (.pem, .crt, .der).
    - [x] Explicación de esquemas de firma de Android (v1, v2 y v3).
- [x] **Punto de Entrada en Configuración (`SettingsScreen`):**
  - [x] Nueva tarjeta destacada en la sección "GUÍA Y APRENDIZAJE CRIPTOGRÁFICO".
  - [x] Navegación desacoplada y fluida en `MainActivity.kt` con animación direccional y respeto de insets edge-to-edge.

---

### 🔮 Fase 7: Firma Móvil de APKs y Zipalign Integrado (Próximo Sprint)
- [ ] Herramienta para seleccionar un APK o AAB no firmado en el almacenamiento del dispositivo.
- [ ] Firma digital móvil con APK Signature Scheme v1 (Jar Signature), v2 (APK Signing Block) y v3.
- [ ] Alineación de 4 bytes (`zipalign`) integrada antes o durante el proceso de firma.
- [ ] Comprobación de firma e integridad de paquetes APK directamente en el teléfono.
