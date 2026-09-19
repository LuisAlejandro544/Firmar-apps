# Keystore Creator 🔐

Una aplicación móvil moderna desarrollada en **Kotlin** y **Jetpack Compose** para generar, gestionar y exportar archivos Keystore (`.jks` / `.keystore` en formato estándar PKCS12) directamente desde un teléfono Android, sin necesidad de una computadora ni herramientas de terminal como `keytool`.

---

## 📱 Características Principales

- **Generador de Keystores Criptográfico:**
  - Creación de pares de claves RSA (2048 y 4096 bits) con certificados X.509 v3 autofirmados.
  - **Soporte Dual de Formatos (`.jks` y `.keystore`):** Selector de extensión estándar Android o clásico/Flutter, con autocompletado y detección inteligente de nombres para que el usuario escriba lo que desee sin preocuparse por escribir la extensión a mano.
  - **Validez Granular de 1 Día a 100 Años con Slider Interactivo:** Control deslizante continuo que permite fijar la validez desde 1 día (ideal para pruebas temporales o debug) hasta 100 años (llaves de producción de largo plazo), con cálculo de fecha de caducidad en tiempo real y accesos rápidos (1 día, 30 días, 1 año, 25 años, 30 años, 100 años).
  - Firma digital con algoritmo `SHA256withRSA` utilizando el proveedor de seguridad nativo de Android.
  - **Generador y Auditor de Contraseñas Ultra Seguras con Motor zxcvbn:**
    - Botón táctil ergonómico con varita mágica al lado de los campos de contraseña en el formulario móvil.
    - Diálogo interactivo con 3 opciones de longitud criptográfica: **16 caracteres** (Alta), **24 caracteres** (Muy Alta) y **32 caracteres** (Ultra Segura).
    - Generación asistida con reintentos automáticos y certificación obligatoria de **Indescifrabilidad (Score 4/4)** mediante la biblioteca estándar de auditoría `zxcvbn`. Se excluyen patrones de teclado, nombres y palabras de diccionario.
    - 100% compatibles con scripts de Gradle, Android Studio y apksigner (sin caracteres problemáticos para bash).
    - **Avisos Nativos de Seguridad en Vivo (Sin Toasts):** Si el usuario ingresa manualmente una contraseña vulnerable o descifrable, la app muestra un componente nativo de advertencia explicativo con el tiempo estimado de descifrado, aconsejando robustecerla pero permitiendo continuar con la firma sin bloqueos forzados.
    - Medidor dinámico de fortaleza en tiempo real con barra de progreso visual de color según nivel de entropía.
    - Botón de regeneración instantánea y vista previa monoespaciada para revisión en pantalla.
  - Campos opcionales del certificado (CN, OU, O, C) con sección colapsable.
  - Opción de "Misma contraseña para la clave" para mayor comodidad al escribir en el teléfono.
  - Botón de **Datos de prueba** para validaciones instantáneas.

- **Cifrado Fuerte de Credenciales en Reposo (Android KeyStore + AES-256-GCM):**
  - **Protección Criptográfica en la Base de Datos:** Las contraseñas nunca se guardan en texto plano en la memoria flash del teléfono ni en SQLite.
  - Cada credencial se cifra de forma transparente mediante **AES-256 en modo GCM (Galois/Counter Mode)** con un Vector de Inicialización (IV) de 12 bytes aleatorio y una etiqueta de autenticación (Tag) de 128 bits para prevenir manipulaciones.
  - La clave maestra está resguardada en el hardware seguro del dispositivo (**Android KeyStore Provider**, respaldado por TEE / StrongBox), impidiendo que otras aplicaciones o volcados del sistema de archivos puedan acceder a las contraseñas sin autorización criptográfica del procesador.
  - Compatibilidad total hacia atrás con registros previos no cifrados.

- **Gestión Local Segura (Room Database):**
  - Registro de todas las keystores creadas con sus credenciales (alias, contraseñas protegidas con cifrado fuerte).
  - Visualización del tamaño del archivo, fecha de creación y algoritmo empleado.
  - Buscador rápido en tiempo real por título, nombre de archivo o alias.
  - Eliminación segura que limpia tanto la base de datos como el archivo físico del almacenamiento interno.

- **Detalles y Exportación para Firma de APKs:**
  - Cálculo automático y copia en 1 toque de huellas digitales **SHA-256** y **SHA-1** (vitales para Firebase, Google Sign-In, Play Console, etc.).
  - **Navegación por Pestañas para Códigos de Integración (`build.gradle.kts` ⟷ GitHub Actions CI/CD):** En la pantalla de detalles, los fragmentos de código de compilación y despliegue están organizados mediante un selector de pestañas (`SecondaryTabRow`) con transiciones animadas laterales (`AnimatedContent`), permitiendo consultar y copiar tanto la configuración de Gradle como el pipeline de GitHub Actions de forma inmediata sin tener que desplazarse verticalmente por la pantalla.
  - Generador de bloques de configuración listos para `build.gradle.kts` y comandos de `apksigner`.
  - **Workflow Completo de GitHub Actions CI/CD en 1 Clic:** Generador de pipeline completo de integración continua listo para pegar en `.github/workflows/build-and-sign.yml`. Incluye todos los datos ya pre-rellenados para la clave seleccionada: nombre de archivo (`.jks` o `.keystore`), alias, variables de entorno de firma, decodificación Base64 desde secretos, compilación con Gradle y firma verificada con apksigner.
  - **Generación de Base64 con 1 Clic (CI/CD):** Conversión directa del almacén de claves a texto codificado en Base64 para su uso inmediato en variables de entorno como `ANDROID_KEYSTORE_BASE64` en GitHub Actions o pipelines de CI/CD. Incluye opciones para copiar la cadena, compartir texto o exportar como archivo físico `.base64`.
  - **Privacidad, Auto-limpieza en 2 Minutos y Notificaciones In-App:**
    - Al copiar contraseñas o la clave Base64, se suprime la notificación nativa predeterminada de Android (`EXTRA_IS_SENSITIVE`) y se muestra un banner in-app exclusivo con diseño Material Design 3.
    - **Limpieza Automática a los 2 Minutos (Estricta):** Al copiar datos desde la app, se inicia un temporizador de 2 minutos (120 s). Al cumplirse el tiempo, el portapapeles se limpia automáticamente **únicamente si aún contiene el dato exacto copiado de nuestra app**. Si el usuario copió texto de otra aplicación (WhatsApp, navegador, etc.), no se borra, garantizando no interferir con otras actividades del usuario.
  - Exportación y compartición directa del archivo (`.jks` o `.keystore`) mediante `FileProvider` a herramientas móviles como MT Manager, Google Drive, WhatsApp, Telegram o el gestor de archivos del teléfono.

- **Menú de Configuración y Personalización Visual 100% Flexible:**
  - **Pantalla Dedicada de Ajustes:** Menú estructurado con tarjetas informativas sobre apariencia, seguridad offline y arquitectura del procesador.
  - **Modo de Apariencia Triple:** Opción para alternar entre "Seguir el sistema" (automático), "Modo claro" y "Modo oscuro".
  - **Soporte Nativo para Material You:** Integración completa con colores dinámicos del sistema en dispositivos con Android 12+ (API 31+). Si el móvil cuenta con una versión anterior, la interfaz lo detecta y explica amigablemente la compatibilidad.
  - **Paleta de Colores 100% Personalizable:**
    - Colección de paletas predefinidas optimizadas (Índigo, Cobalto, Océano, Cian, Esmeralda, Menta, Naranja, Ámbar, Carmesí, Fucsia, Púrpura, Grafito).
    - Selector granular con controles deslizantes RGB independientes (0-255) y visualizador de código hexadecimal en tiempo real (`#RRGGBB`).
    - Botón de restablecimiento al color original de la aplicación.
  - **Vista Previa en Vivo:** Tarjeta dinámica que renderiza botones y componentes con el color y tema seleccionados en tiempo real antes de salir.
  - **Persistencia Reactiva:** Cambios guardados al instante con `ThemePreferences` y propagados fluidamente por toda la app mediante `ThemeViewModel`.
  - **Tipografía y Escala de Fuente Fija (`fontScale = 1.0f`):** Se desacopla la escala de texto de la configuración de accesibilidad del teléfono para proteger el diseño visual, evitando rupturas, cortes de palabras o desbordamientos en tarjetas y botones, manteniendo intacta la escala de píxeles por densidad.
  - **Alineación Edge-to-Edge Perfeccionada:** Gestión de insets sin duplicación en pantallas secundarias (`KeystoreDetailScreen`, `ColorPickerScreen`), garantizando barras superiores perfectamente pegadas a la barra de estado sin espacios vacíos.
  - **Sistema de Transiciones y Micro-animaciones Nativas (Jetpack Compose):**
    - **Navegación Fluida entre Pestañas:** Transición con deslizamiento direccional (`slideInHorizontally` / `slideOutHorizontally` + `fadeIn` / `fadeOut`) y curvas de aceleración `FastOutSlowInEasing` que responden a la posición relativa de las pestañas (Generador ⟷ Almacén / Historial ⟷ Ajustes).
    - **Navegación en Profundidad (Detalles y Selectores):** Entrada y salida con deslizamiento horizontal natural al abrir el detalle de una keystore o el selector de color de tema.
    - **Micro-animaciones Reactivas en Tarjetas (`animateContentSize`):** Expansión y contracción suave y progresiva en tarjetas de formulario, credenciales cifradas, exportación Base64 y generación de contraseñas, evitando cambios bruscos de altura en la interfaz táctil.

- **Crypto Lab: Herramientas de Depuración y Auditoría Independientes:**
  - **Doble Lanzador en el Sistema:** Dispone de su propia actividad independiente (`DebugToolsActivity`) con icono dedicado en el cajón de aplicaciones de Android ("Crypto Lab (Debug)") y acceso directo desde los Ajustes de la aplicación.
  - **1. Inspector Forense de Certificados (ASN.1 & Fingerprints):**
    - **Verificación Criptográfica Real (Sin Simulación):** Firma digitalmente en memoria un payload dinámico utilizando la clave privada con el algoritmo `SHA256withRSA` y verifica de forma inmediata el resultado matemático con la clave pública del certificado X.509 (`verifier.verify()`), garantizando autenticidad absoluta del par de claves.
    - **Auditoría Estructural ASN.1:** Desglose del número de serie en hexadecimal y decimal, tamaño del módulo RSA en bits (2048/4096), exponente público (`65537 / 0x10001`), OID de firma (`1.2.840.113549.1.1.11`), fechas de validez con cálculo de días restantes y comprobación en vivo con `checkValidity()`.
    - **Extracción de Huellas Forenses:** Cálculo byte a byte de huellas **SHA-256**, **SHA-1** y **MD5** con botón de copiado rápido al portapapeles.
    - **Inspección de Sujeto X.500:** Mapeo de atributos RDN (`CN`, `OU`, `O`, `C`) y detalles físicos del archivo en disco.
    - **Auditoría al Vuelo:** Opción para generar y auditar almacenes efímeros de prueba al instante.
  - **2. Benchmark de Rendimiento Criptográfico (32-bit vs 64-bit):**
    - **Telemetría de Hardware en Vivo:** Detecta en tiempo real el procesador del teléfono, versión de Android, ABIs soportadas (`arm64-v8a`, `armeabi-v7a`, `x86_64`), núcleos de CPU, memoria RAM de la JVM y si el proceso opera en modo nativo de **64 bits** o **32 bits**.
    - **Prueba de Rendimiento Comparativa (RSA 2048 vs 4096 bits):** Mide en milisegundos reales el tiempo de:
      1. Generación de entropía segura con `SecureRandom`.
      2. Generación del par de claves RSA (`KeyPairGenerator`).
      3. Construcción y auto-firma del certificado X.509 v3.
      4. Serialización y almacenamiento PKCS12 en memoria.
      5. Firma asimétrica y verificación digital en vivo.
    - **Evaluación y Ratios:** Muestra el multiplicador de cálculo de números primos grandes (ej. 4096 bits frente a 2048 bits) y diagnostica el aprovechamiento de instrucciones vectoriales NEON/Crypto en 64 bits y la compatibilidad fluida en procesadores de 32 bits.

- **Pipeline de Compilación Automatizada en GitHub Actions (`.github/workflows/build-debug-apk.yml`):**
  - **Firma Generada en Vivo desde Cero:** No espera firmas preexistentes ni requiere secrets. Ejecuta obligatoriamente el script `scripts/generate-debug-keystore.sh` que purga cualquier residuo previo y genera con `keytool` una nueva firma en formato PKCS12 / JKS (`debug.keystore`) con par asimétrico RSA de 2048 bits y validez de 10.000 días.
  - **Compilación Estrictamente Sin Caché:** Configuración con caché desactivada en JDK y Gradle (`cache-disabled: true`, `--no-daemon`, `--no-build-cache`), garantizando builds 100% limpios y reproducibles.
  - **Splits Nativos de Gradle (`splits.abi`) y Artefactos Individuales por Arquitectura:** En lugar de distribuir un binario universal que contenga todas las librerías nativas, Gradle divide y empaqueta de forma estricta las arquitecturas (`armeabi-v7a`, `arm64-v8a`, `x86_64` y `universal`). Cada APK contiene en su carpeta interna `lib/` única y exclusivamente los binarios de su arquitectura:
    - 📱 `APK-Debug-arm64-v8a`: Binario ligero exclusivo para procesadores móviles modernos de 64 bits (solo `lib/arm64-v8a/`).
    - 📱 `APK-Debug-armeabi-v7a`: Binario ligero exclusivo para procesadores móviles de 32 bits o dispositivos económicos (solo `lib/armeabi-v7a/`).
    - 💻 `APK-Debug-x86_64`: Binario exclusivo para emuladores Android y computadoras (solo `lib/x86_64/`).
    - 🌐 `APK-Debug-Universal`: Binario unificado que incluye todas las arquitecturas del mercado.
  - **Disparador Manual y Automático:** Configurado con evento `workflow_dispatch` para compilar con un solo clic desde la web de GitHub en el móvil, y automáticamente tras cada `push` a las ramas principales.

- **Script de Control de Firma Forzada (`scripts/generate-debug-keystore.sh`):**
  - Script Bash ejecutable con manejo de errores estricto (`set -euo pipefail`).
  - Purga residuos de `debug.keystore` tanto en la raíz como en `app/`.
  - Crea el par de claves RSA con `keytool`, verifica su tamaño en bytes y extrae en consola las huellas digitales SHA-256, SHA-1 y MD5 para auditoría en los logs de GitHub Actions.

---

## 🛠️ Stack Tecnológico

| Componente | Tecnología |
|---|---|
| **Lenguaje** | Kotlin 2.2 |
| **Interfaz de Usuario** | Jetpack Compose (Material 3) |
| **Arquitectura** | MVVM (Model-View-ViewModel) con Flow reactivo |
| **Base de Datos Local** | AndroidX Room con KSP |
| **Cifrado en Reposo** | Android KeyStore + AES-256-GCM (Hardware TEE/StrongBox) |
| **Generador de Entropía** | SecureRandom (Criptográficamente Seguro, 16/24/32 caracteres) |
| **Criptografía X.509/PKCS12** | Bouncy Castle (`bcprov-jdk18on`, `bcpkix-jdk18on`) + Conscrypt Android |
| **Navegación** | Navigation Compose |
| **Compartición de Archivos** | AndroidX FileProvider |

---

## 🚀 Requisitos de Compilación

- **SDK Mínimo (minSdk):** Android 7.0 (API 24)
- **SDK Objetivo (targetSdk / compileSdk):** Android 16 (API 36)
- **Java Compatibility:** Java 11 / Java 17
- **Gradle:** 9.x con Kotlin DSL

---

## 📦 Cómo Probar la Aplicación

1. Abre la aplicación en el emulador o instálala en tu dispositivo Android.
2. En la pestaña **Generador**, pulsa el botón *"Datos de prueba"* para autocompletar las credenciales o escribe las tuyas propias.
3. Presiona **Generar Keystore**. En pocos segundos se creará el par de claves RSA y el certificado autofirmado.
4. En el diálogo de confirmación, pulsa **Ver Detalles** para revisar las huellas SHA-256/SHA-1 y el código de Gradle, o **Compartir** para exportar el archivo físico `.jks`.
5. En la pestaña **Mis Keystores**, consulta y administra todos los almacenes generados con el buscador.
