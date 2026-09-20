# Roadmap del Proyecto: Keystore Creator 🗺️

Este documento define el plan de evolución y las próximas funcionalidades de la aplicación.

---

## 🎯 Próximo Paso Prioritario (Siguiente Implementación)

### 📌 Importador e Inspector de Keystores Externas (Fase 3)
* **Objetivo:** Permitir al usuario seleccionar e importar archivos `.jks` o `.keystore` ya existentes en su dispositivo móvil para inspeccionar sus alias, validar contraseñas, extraer huellas SHA-1 / SHA-256 y convertirlas a Base64.
* **Casos de uso clave:**
  - **Inspección sin PC:** Verificar qué claves contiene una keystore antigua sin necesidad de ejecutar `keytool` en una terminal de ordenador.
  - **Validación de credenciales:** Probar si una contraseña coincide antes de compilar o firmar un APK.
  - **Conversión de almacenes antiguos a Base64:** Permitir codificar keystores previas para pipelines de CI/CD modernos.

---

## 📅 Estado de las Fases

### ✅ Fase 1: Fundamentos y Validación (Completada)
- [x] Motor criptográfico para generación de claves RSA (2048 y 4096 bits).
- [x] Construcción y firma de certificados X.509 autofirmados con validez configurable.
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

### ⏳ Fase 6: Importador e Inspector de Keystores Externas (Próximo Sprint)
- [ ] Selector de archivos para importar keystores existentes desde el almacenamiento del teléfono.
- [ ] Extracción y visualización de certificados, alias y huellas de archivos externos.
- [ ] Verificación de contraseñas de almacén y de clave para keystores importadas.

---

### 🔮 Fase 7: Firma Móvil de APKs
- [ ] Herramienta para seleccionar un APK no firmado en el dispositivo y firmarlo con una de las keystores guardadas mediante v1, v2 y v3 scheme.
- [ ] Alineación con `zipalign` integrada.
