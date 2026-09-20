# Estructura del Proyecto: Keystore Creator 📂

Este documento describe la organización modular de los paquetes, capas arquitectónicas y archivos del código fuente.

---

## 🏛️ Patrón Arquitectónico: MVVM Modular

La aplicación sigue el patrón **Model-View-ViewModel (MVVM)** con separación de responsabilidades y flujo unidireccional de datos (`StateFlow`):

```
UI (Compose) ──> ViewModel (StateFlow) ──> Repository ──┬──> Room Database (SQLite)
                                                        └──> Crypto Engine (Bouncy Castle)
```

---

## 🌳 Árbol de Directorios y Archivos

```
.github/workflows/
└── build-debug-apk.yml                  # Pipeline CI/CD para compilar APKs Debug sin caché y por arquitectura

scripts/
├── generate-debug-keystore.sh           # Script Bash para forzar la creación de firma debug desde cero
└── isolate-and-sign-abi-apk.py          # Script Python para purgar librerías nativas cruzadas, zipalign y apksigner v2/v3

app/src/main/
├── AndroidManifest.xml                  # Declaración de actividades y FileProvider
├── java/com/example/
│   ├── MainActivity.kt                  # Actividad principal y Scaffold con NavHost
│   │
│   ├── crypto/                          # Motor Criptográfico y Utilidades
│   │   ├── KeystoreGenerator.kt         # Generador de pares de claves RSA/ECDSA y certificados X.509
│   │   ├── KeystoreFormatConverter.kt   # Conversión bidireccional JKS ⟷ PKCS12 (.p12 / .keystore)
│   │   ├── KeystoreExportHelper.kt      # Compartición mediante FileProvider, copias, SAF y snippets
│   │   ├── CertificateExportHelper.kt   # Extracción segura de certificados X.509 (.pem, .crt, .der)
│   │   ├── StorageCompressionHelper.kt  # Motor de compresión Deflate/Gzip y empaquetador ZIP All-in-One
│   │   ├── ZipImportHelper.kt           # Motor de importación, protección Zip-Slip, parser heurístico y validación
│   │   ├── SecureCredentialsCipher.kt   # Cifrado AES-256-GCM respaldado por Android KeyStore (hardware TEE)
│   │   └── PasswordSecurityEngine.kt    # Generador de contraseñas de alta entropía (16, 24, 32 caracteres)
│   │
│   ├── data/                            # Capa de Persistencia y Modelos
│   │   ├── dao/
│   │   │   └── KeystoreDao.kt           # Consultas SQL con Room (CRUD y Flow reactivo)
│   │   ├── database/
│   │   │   └── AppDatabase.kt           # Instancia singleton de la base de datos Room
│   │   ├── model/
│   │   │   └── KeystoreEntity.kt        # Entidad de tabla con credenciales y metadatos
│   │   └── repository/
│   │       └── KeystoreRepository.kt    # Capa intermedia con cifrado transparente en SQLite y limpieza en disco
│   │
│   └── ui/                              # Capa de Presentación (Jetpack Compose)
│       ├── detail/
│       │   ├── KeystoreDetailScreen.kt  # Vista de detalle, credenciales, huellas y exportación
│       │   └── KeystoreDetailViewModel.kt # Lógica de revelación de contraseñas y eliminación
│       ├── generator/
│       │   ├── GeneratorScreen.kt       # Formulario interactivo para crear nuevas keystores
│       │   └── GeneratorViewModel.kt    # Validación de entradas y coordinación en segundo plano
│       ├── importzip/
│       │   ├── ZipImportScreen.kt       # Interfaz de restauración SAF, diagnóstico de ZIP y confirmación
│       │   └── ZipImportViewModel.kt    # Máquina de estados de análisis, auto-rellenado y guardado en Room
│       ├── list/
│       │   ├── KeystoreListScreen.kt    # Listado con buscador, acceso a importación ZIP y estado vacío
│       │   └── KeystoreListViewModel.kt # Filtrado en tiempo real y confirmación de borrado
│       ├── navigation/
│       │   └── NavRoutes.kt             # Definición tipada de rutas y destinos de barra inferior
│       ├── settings/
│       │   ├── SettingsScreen.kt        # Menú principal de configuración, seguridad y sistema
│       │   ├── CryptoGuideScreen.kt     # Guía y preguntas frecuentes (estándares móviles, formatos y algoritmos)
│       │   ├── ColorPickerScreen.kt     # Selector de tema, Material You, paletas y RGB granular
│       │   ├── ThemePreferences.kt      # Persistencia en SharedPreferences con StateFlow
│       │   └── ThemeViewModel.kt        # Estado global del tema para la aplicación
│       ├── debug/
│       │   ├── DebugToolsActivity.kt    # Actividad independiente con launcher icon para Crypto Lab
│       │   ├── CertificateForensicScreen.kt    # Interfaz del Inspector Forense ASN.1 y Firma Real
│       │   ├── CertificateForensicViewModel.kt # Verificación matemática con SHA256withRSA y X.509
│       │   ├── CryptoBenchmarkScreen.kt        # Interfaz del Benchmark de Carga 32-bit vs 64-bit
│       │   └── CryptoBenchmarkViewModel.kt     # Telemetría de procesador y medición de fases RSA
│       └── theme/
│           ├── Color.kt                 # Paleta de colores índigo/cian de seguridad
│           ├── Theme.kt                 # Tema Material Design 3 con escala de fuentes fija (fontScale = 1.0f)
│           └── Type.kt                  # Tipografía de la aplicación
│
└── res/
    ├── drawable/                        # Vectores del icono del launcher, Crypto Lab y fondos
    ├── mipmap-anydpi-v26/               # Icono adaptativo oficial
    ├── values/
    │   ├── strings.xml                  # Cadenas de texto localizadas
    │   └── colors.xml                   # Colores de apoyo
    └── xml/
        └── file_paths.xml               # Configuración segura de rutas para FileProvider
```

---

## 🔍 Responsabilidad de Cada Módulo

### 1. `crypto/`
* **`KeystoreGenerator.kt`**: Implementa la lógica criptográfica sin depender de `keytool` de PC. Soporta generación asimétrica dual: pares de claves RSA (2048/4096 bits) y **Curvas Elípticas ECDSA** (`secp256r1 / NIST P-256`, `secp384r1 / NIST P-384`, `secp521r1 / NIST P-521`). Emite certificados **X.509 v3** estándar (RFC 5280) con firma adaptativa (`SHA256withRSA`, `SHA256withECDSA`, `SHA384withECDSA`, `SHA512withECDSA`) y extensiones canónicas: `BasicConstraints(false)` crítica, `KeyUsage(digitalSignature)` crítica y generación de `SubjectKeyIdentifier` y `AuthorityKeyIdentifier`. Permite guardar directamente en formatos `.jks`, `.keystore` y `.p12` (PKCS#12 universal).
* **`KeystoreFormatConverter.kt`**: Utilidad criptográfica especializada en la conversión bidireccional entre almacenes Java KeyStore (`JKS`) y `PKCS12 (.p12 / .keystore)` sin alterar la clave privada subyacente ni invalidar la cadena de certificados X.509 y sus huellas digitales SHA-1 y SHA-256. Permite opcionalmente redefinir o conservar contraseñas de almacén y alias, y registra la nueva llave en Room cifrada con AES-256-GCM.
* **`SecureCredentialsCipher.kt`**: Gestor de cifrado y descifrado de credenciales sensibles en reposo mediante **AES-256-GCM** autenticado, respaldado por hardware seguro a través de **Android KeyStore Provider** (TEE / StrongBox). Incorpora generación de IV de 12 bytes aleatorio, tag de autenticación de 128 bits, empaquetado Base64 multiplataforma y compatibilidad hacia atrás transparente con registros preexistentes en texto plano.
* **`PasswordSecurityEngine.kt`**: Motor de generación y auditoría de contraseñas de alta entropía asistido por `SecureRandom` y el analizador de algoritmos de diccionario `zxcvbn`. Ofrece 3 longitudes estándar (16, 24 y 32 caracteres) con distribución garantizada de mayúsculas, minúsculas, dígitos y símbolos no ambiguos (evitando caracteres conflictivos en compilaciones de Gradle y terminales), asegurando score 4/4 (indescifrables). Incluye auditoría en tiempo real para alertar de forma nativa en la UI si el usuario introduce contraseñas vulnerables a descifrado.
* **`KeystoreExportHelper.kt`**: Proporciona métodos para:
  - Compartir de forma segura el archivo físico con otras aplicaciones (vía `FileProvider` con permisos `FLAG_GRANT_READ_URI_PERMISSION`).
  - Guardar directamente en el almacenamiento seleccionado por el usuario mediante el Storage Access Framework (SAF - `writeKeystoreToUri`, `writeBase64ToUri`, `writeGradleSnippetToUri`, `writeGitHubWorkflowToUri`).
  - Convertir el archivo a texto **Base64** (`generateBase64`), compartirlo como texto plano, exportarlo como archivo `.base64` o escribirlo vía SAF.
  - Copiar textos al portapapeles suprimiendo la notificación nativa de Android 13+ con `ClipDescription.EXTRA_IS_SENSITIVE` para credenciales sensibles.
  - **Auto-limpieza a los 2 minutos:** Registra marcas distintivas en el `ClipData` y programa un `Handler` que a los 120 segundos inspecciona el portapapeles actual y **solo lo vacía si aún contiene el dato exacto copiado desde la app**, sin tocar contenido que el usuario haya copiado posteriormente en otras aplicaciones.
  - Generar el bloque de configuración `signingConfigs` para `build.gradle.kts` y el comando de `apksigner`.
  - **Generar Workflow Completo de GitHub Actions (`generateFullGitHubActionWorkflow`):** Pipeline preconfigurado de CI/CD para compilar y firmar APKs automáticamente con la clave seleccionada.
* **`CertificateExportHelper.kt`**: Aísla criptográficamente el certificado público X.509 v3 desde el almacén PKCS12 / JKS omitiendo cualquier clave privada o credencial. Ofrece serialización en 3 formatos estándar (PEM con encabezados RFC 7468 a 64 columnas, binario CRT y binario nativo DER) con utilidades de compartición (`FileProvider`) y guardado directo (`Storage Access Framework`).
* **`StorageCompressionHelper.kt`**: Motor de compresión y empaquetado ultra-eficiente que utiliza `Deflater` en nivel máximo (`BEST_COMPRESSION`), compresión `GZIP` y empaquetado multi-archivo `ZipOutputStream` con búferes optimizados de 8 KB. Genera el paquete integral All-in-One (`.zip`) conteniendo la keystore (.jks/.keystore), certificados (.pem, .crt), archivo Base64 (.base64), script CI/CD (.yml), bloque Gradle (.gradle.kts) y reporte de auditoría (`info.txt`). Incluye utilidades para compartir vía `FileProvider`, escribir directamente a URIs de SAF y computar porcentajes de ahorro de almacenamiento en disco en reposo.
* **`ZipImportHelper.kt`**: Motor autónomo de extracción e inspección para paquetes ZIP y almacenes de claves. Implementa sandbox de descompresión segura en caché con verificación canónica contra vulnerabilidades Zip-Slip, detección heurística de artefactos (.jks, .keystore, .pem, .crt, .base64, gradle, yml), reconstrucción de almacenes a partir de cadenas Base64, parser automático de credenciales desde `signingConfigs.gradle.kts` o `INFO_KEYSTORE.txt`, validación criptográfica estricta con `java.security.KeyStore` (PKCS12 con fallback a JKS), extracción forense del certificado X.509 y cálculo de huellas digitales SHA-256 y SHA-1.

### 2. `data/`
* **`KeystoreEntity.kt`**: Modela los datos de la keystore: ID, título, nombre de archivo, ruta absoluta, alias, contraseñas, algoritmo, tamaño en bytes, validez en años y huellas.
* **`KeystoreDao.kt`**: Consultas Room para inserción, lectura ordenada por fecha descendente, búsqueda por ID y borrado.
* **`KeystoreRepository.kt`**: Repositorio central que intercepta todas las operaciones: cifra las contraseñas con `SecureCredentialsCipher` antes de persistir en Room, las descifra al vuelo al consultarlas, y al borrar un registro elimina de forma segura el archivo físico del disco para no saturar la memoria del teléfono.

### 3. `ui/`
* **`MainActivity.kt`**: Contenedor principal con `NavHost`, `CenterAlignedTopAppBar` y `NavigationBar`. Implementa acondicionamiento quirúrgico de `contentWindowInsets` para que pantallas secundarias (`KeystoreDetailScreen`, `ColorPickerScreen`) gobiernen su propio `Scaffold` y `TopAppBar` pegados a la barra de estado, sin duplicación de insets ni espacios residuales.
* **`GeneratorScreen.kt` & `GeneratorViewModel.kt`**: Pantalla de creación asistida con selector de algoritmo asimétrico (RSA 2048/4096 vs ECDSA Curvas Elípticas P-256, P-384, P-521), selector triple de formato (`.jks`, `.keystore` y `.p12`), autocompletado y sufijo inteligente de nombres de archivo, control deslizante continuo de validez (1 día a 100 años) con cálculo dinámico de caducidad y accesos rápidos, generador de contraseñas ultra seguras asistido por `zxcvbn`, datos de prueba y validaciones.
* **`KeystoreListScreen.kt` & `KeystoreListViewModel.kt`**: Lista de keystores con buscador en vivo, acceso táctil a la restauración de paquetes ZIP y tarjeta con metadata esencial.
* **`ZipImportScreen.kt` & `ZipImportViewModel.kt`**: Flujo completo de restauración e importación de paquetes ZIP y keystores externas. Integra selector de documentos SAF (`OpenDocument`), descompresión aislada con prevención anti Zip-Slip, resumen visual de los artefactos detectados en el ZIP con chips de colores, formulario con autocompletado inteligente de credenciales (extraídas de `signingConfigs` o `info.txt`), validación de clave privada con `KeyStore` nativo, extracción de huellas SHA-256 y guardado en Room con cifrado AES-256-GCM.
* **`KeystoreDetailScreen.kt` & `KeystoreDetailViewModel.kt`**:
  - Visualización completa de credenciales y copiado seguro con auto-limpieza a los 2 minutos.
  - **Conversión de Formatos (JKS ⟷ PKCS12 / .p12):** Herramienta integrada con diálogo interactivo `KeystoreConvertDialog` que permite convertir la llave actual a otro formato de almacén sin modificar las huellas digitales del certificado, con personalización de nombres y contraseñas.
  - **Identificación de Formato y Algoritmo:** Detección en vivo de si el archivo es JKS, PKCS12 (.p12) o .keystore y badges para firmas con Curvas Elípticas (ECDSA).
  - **Paquete Completo Comprimido All-in-One (.zip):** Tarjeta dedicada con barra de progreso, generación de ZIP ultra-comprimido, opción de guardado nativo con Storage Access Framework (`CreateDocument`) y botón para compartir mediante `FileProvider`.
  - **Exportación Individual SAF:** Botones dedicados para guardar en almacenamiento nativo el almacén (`.jks`/`.keystore`), la cadena Base64 (`.base64`), el bloque Gradle (`.gradle.kts`) y el pipeline CI/CD (`.yml`).
  - **Telemetría de Compresión Deflate:** Visualización en tiempo real del tamaño en bytes original frente al tamaño con compresión máxima y porcentaje de reducción en disco.
  - **Exportación de Certificados X.509:** Pestañas animadas para formatos PEM, CRT y DER con opciones de copia, compartición y descarga SAF.
  - Tarjeta con Workflow completo de GitHub Actions CI/CD con selector de pestañas animadas (`SecondaryTabRow`).
  - Generación de Base64 con 1 solo clic para CI/CD con visor integrado y opciones de exportación.
  - Sistema de notificaciones in-app de seguridad (`SecurityNotificationBanner`) que advierte sobre los riesgos del portapapeles sin depender de toasts nativos de Android.
* **`SettingsScreen.kt` & `CryptoGuideScreen.kt` & `ColorPickerScreen.kt` & `ThemeViewModel.kt`**:
  - `SettingsScreen.kt`: Menú de opciones organizado por categorías (Apariencia, Guía y Aprendizaje Criptográfico, Seguridad/Almacenamiento, Información técnica del sistema y Acceso directo a Crypto Lab).
  - `CryptoGuideScreen.kt`: Pantalla dedicada de Guía Criptográfica y FAQ para desarrolladores móviles sin PC. Dispone de buscador en tiempo real, filtro por chips de categorías (Formatos, Algoritmos, Validez, Seguridad), tarjetas con acordeón interactivo y respuestas profundas sobre los formatos recomendados por Google y la industria (.jks vs .p12 vs .keystore), comparativas RSA vs ECDSA, validez de 25-100 años, esquemas v1/v2/v3 y gestión segura de firmas.
  - `ColorPickerScreen.kt`: Pantalla dedicada para personalización del tema. Permite alternar entre Seguir el sistema, Modo claro y Modo oscuro, activar Material You (colores dinámicos en Android 12+), seleccionar paletas predefinidas o definir cualquier color RGB al 100% con vista previa en vivo.
  - `ThemePreferences.kt`: Almacenamiento local mediante `SharedPreferences` que expone un `StateFlow` reactivo para que los cambios se reflejen de inmediato en toda la aplicación.
* **`com.example.ui.debug` (Crypto Lab Tools)**:
  - `DebugToolsActivity.kt`: Actividad independiente declarada con su propio launcher icon (`ic_debug_launcher`) en `AndroidManifest.xml` para aparecer de forma autónoma en el cajón de aplicaciones de Android bajo el nombre "Crypto Lab (Debug)".
  - `CertificateForensicScreen.kt` & `CertificateForensicViewModel.kt`: Inspector forense que realiza una prueba de firma matemática real con `SHA256withRSA` sobre un payload en memoria validándolo contra la clave pública del certificado X.509 para comprobar la autenticidad del par de claves, desglosa la estructura ASN.1 (número de serie, módulo RSA, exponente, OID, fechas de validez), calcula las huellas SHA-256, SHA-1 y MD5 byte a byte, y permite auditar claves efímeras al vuelo.
  - `CryptoBenchmarkScreen.kt` & `CryptoBenchmarkViewModel.kt`: Benchmark de carga que inspecciona la telemetría del procesador móvil (ABIs soportadas, núcleos, RAM y modo 32-bit vs 64-bit), ejecuta pruebas cronometradas por fases (entropía, generación de clave, emisión de certificado X.509, empaquetado PKCS12 y firma) para RSA 2048 y 4096 bits, y genera diagnósticos de rendimiento para el chip del dispositivo.

### 4. Automatización, CI/CD y Scripts (`.github/` y `scripts/`)
* **`scripts/generate-debug-keystore.sh`**:
  - Script Bash ejecutable con directivas de seguridad (`set -euo pipefail`).
  - Purga firmas `debug.keystore` previas tanto en la raíz como en `app/` para forzar la creación de un par de claves limpio y único en cada compilación.
  - Invoca `keytool` del JDK con RSA de 2048 bits, validez de 10.000 días y genera las huellas forenses SHA-256, SHA-1 y MD5 en la consola de compilación.
* **`scripts/isolate-and-sign-abi-apk.py`**:
  - Script en Python 3 para purga e inmunización estricta de arquitecturas nativas.
  - Examina el APK y descarta de forma quirúrgica cualquier archivo `.so` en `lib/` que pertenezca a otra arquitectura (ej. elimina `armeabi-v7a`, `x86`, `x86_64` de los binarios `arm64-v8a`).
  - Purga firmas residuales previas en `META-INF/` para prevenir firmas inválidas o corruptas.
  - Ejecuta `zipalign -f -p 4` para alinear datos y bibliotecas compartidas a límites de 4 bytes / páginas.
  - Re-firma el APK con `apksigner` aplicando los esquemas modernos v2 y v3 con `debug.keystore`.
  - Audita el paquete final con verificación "fail-fast": si se detecta una sola librería nativa ajena, detiene la ejecución inmediatamente con error.
* **`.github/workflows/build-debug-apk.yml`**:
  - Pipeline de GitHub Actions ejecutado en contenedores limpios `ubuntu-latest`.
  - Configura Java 17 y Gradle con la caché desactivada (`cache-disabled: true`, `--no-daemon`, `--no-build-cache`).
  - Ejecuta `generate-debug-keystore.sh` para auto-firmar la compilación sin depender de variables de entorno ni secrets.
  - Ejecuta `isolate-and-sign-abi-apk.py` en el paso de empaquetado para aislar cada arquitectura (`arm64-v8a`, `armeabi-v7a`, `x86_64` y `universal`), erradicando que en `lib/` se mezclen binarios de otras arquitecturas.
  - Sube cada paquete como un artefacto individual mediante `actions/upload-artifact@v4`, permitiendo descargarlos directamente desde el teléfono sin lidiar con archivos zip comprimidos monolíticos y con el menor tamaño posible.
