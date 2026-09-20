# Contexto para Asistentes de IA (AI Context) 🤖

Este archivo contiene el contexto técnico, limitaciones del entorno y directrices de dominio para cualquier modelo o agente de IA que colabore en este proyecto.

---

## 🎯 Propósito del Proyecto
Keystore Creator es una aplicación nativa para Android cuyo objetivo es permitir a desarrolladores y creadores generar, gestionar y exportar archivos de almacén de claves (`keystores` `.jks` / `.keystore`) directamente desde sus dispositivos móviles, eliminando la dependencia de una PC o de la consola de comandos de Java (`keytool`).

---

## 📱 Contexto del Usuario y Entorno de Ejecución
1. **El usuario opera únicamente desde un teléfono móvil:**
   - No tiene acceso a una computadora de escritorio ni terminal ADB.
   - Las soluciones deben poderse validar, probar y compartir desde la pantalla táctil de un smartphone.
   - La experiencia de usuario debe priorizar flujos cómodos con teclados móviles (botones de autocompletado de prueba, botones para copiar al portapapeles en 1 toque, selectores desplegables y alternadores de visibilidad).

2. **Distribución fuera de Google Play Store:**
   - El APK está destinado a tiendas alternativas y distribución directa (ej: Uptodown, descarga de APK de terceros).
   - No se aplican restricciones arbitrarias de Google Play Console sobre herramientas de desarrollo o firmas, pero se debe mantener la seguridad de datos del usuario.

3. **Arquitectura y compatibilidad de procesadores:**
   - Debe funcionar sin problemas en arquitecturas de **32 bits (armeabi-v7a)** y **64 bits (arm64-v8a, x86_64)**.
   - No utilizar librerías nativas binarias (.so) que no cubran ambas arquitecturas.

---

## 🔐 Peculiaridades Criptográficas en Android (CRÍTICO)

### 1. Formato PKCS12 vs JKS Clásico y Soporte Dual (.jks y .keystore)
- En versiones modernas de Android y Java (JDK 9+), el formato predeterminado y recomendado por Google para `apksigner` es **PKCS12** (tanto con extensión `.jks` como `.keystore`).
- La aplicación soporta explícitamente ambas extensiones (`.jks` y `.keystore`). Al escribir en el campo de nombre, el sistema detecta si el usuario escribió la extensión y la separa automáticamente, evitando nombres duplicados como `mi_llave.jks.jks` o `mi_llave.keystore.keystore`.
- Bouncy Castle y el runtime de Android manejan PKCS12 con soporte completo para almacenar la clave privada RSA y la cadena de certificados X.509 v3.

### 2. Rango de Validez Granular (1 día a 100 años)
- La validez del certificado X.509 se calcula en días exactos con `Calendar.add(Calendar.DAY_OF_YEAR, totalDays)`, permitiendo un rango continuo desde 1 día (para pruebas rápidas de depuración) hasta 100 años (36,500 días para firmas de largo plazo).

### 3. Conflicto de Proveedores de Seguridad (`BC` vs Conscrypt)
- **Lección aprendida / Bug resuelto:** Android incluye internamente una implementación antigua y mutilada de Bouncy Castle bajo el nombre de proveedor `"BC"`.
- **Regla obligatoria:** Nunca forzar `.setProvider(BouncyCastleProvider.PROVIDER_NAME)` ni `.setProvider("BC")` para constructores de firma (`JcaContentSignerBuilder`) o conversores de certificados (`JcaX509CertificateConverter`). Debe permitirse al sistema utilizar su proveedor nativo (`Conscrypt`/`OpenSSL`) o pasar la instancia directa en memoria (`BouncyCastleProvider()`) como respaldo.

### 3. Almacenamiento y Compartición de Archivos (Zero Permissions)
- Las keystores generadas se guardan en el almacenamiento interno privado de la aplicación: `context.filesDir/keystores/`.
- No solicitar permisos peligrosos como `READ_EXTERNAL_STORAGE` o `WRITE_EXTERNAL_STORAGE`.
- La exportación hacia otras aplicaciones (gestores de archivos, mensajería, nube) se realiza mediante **`androidx.core.content.FileProvider`** con el URI de contenido `content://com.example.fileprovider/...`.

### 4. Seguridad del Portapapeles, Auto-limpieza en 2 Minutos y Notificaciones In-App
- **Protección en Android 13+ (API 33+):** Al copiar información sensible (contraseñas de keystore/clave o cadenas Base64 con la clave privada), se agrega el extra `ClipDescription.EXTRA_IS_SENSITIVE = true` en el `ClipData`. Esto le indica al sistema Android que oculte la vista previa flotante nativa y evite exponer la contraseña en texto plano en la pantalla.
- **Auto-limpieza a los 2 minutos (Estricta):** Al copiar datos desde la aplicación, se programa una tarea con `Handler(Looper.getMainLooper())` a los 120 segundos. Al ejecutarse, verifica minuciosamente que el portapapeles siga conteniendo la metadata de la app (`com.example.IS_KEYSTORE_APP_CLIP`) y el texto exacto copiado; si y solo si ambas condiciones se cumplen, se invoca `clipboard.clearPrimaryClip()` (o texto vacío en APIs < 28). Si el usuario copió datos de otra app entretanto, no se borra.
- **Avisos in-app personalizados:** Se evitan los `Toast.makeText` nativos (que son toscos y no personalizables). En su lugar, la app utiliza un componente visual in-app (`SecurityNotificationBanner`) que advierte al usuario explícitamente: *"Otras aplicaciones instaladas en tu dispositivo podrían acceder al portapapeles. Ten precaución dónde lo pegas."*

### 5. Conversión a Base64 para CI/CD
- La codificación a Base64 se realiza mediante `android.util.Base64.encodeToString(bytes, Base64.NO_WRAP)` para evitar saltos de línea indeseados que rompan secretos en GitHub Actions (`ANDROID_KEYSTORE_BASE64`) o GitLab CI.

### 6. Cifrado de Credenciales en Reposo (Android KeyStore + AES-256-GCM)
- **Problema abordado:** Si un teléfono es rooteado, inspeccionado forensemente o si se realiza un backup de la app, una base de datos SQLite estándar contendría las contraseñas del keystore en texto plano.
- **Arquitectura implementada:** `SecureCredentialsCipher` utiliza el hardware seguro **AndroidKeyStore** (TEE/StrongBox) para generar y resguardar una clave maestra AES de 256 bits (`KeystoreVaultMasterKey`).
- Cada contraseña se cifra con `AES/GCM/NoPadding` generando un IV aleatorio de 12 bytes (`SecureRandom`) y un tag de autenticación de 128 bits.
- El formato almacenado en Room es `enc:v1:<Base64(IV + CipherText + Tag)>`.
- **Compatibilidad hacia atrás:** Si el texto no inicia con `enc:v1:`, el motor sabe que es un registro heredado y lo descifra/retorna en texto plano de forma transparente.
- En `KeystoreRepository`, el cifrado y descifrado se ejecutan de manera transparente al escribir o leer de `KeystoreDao`.

### 7. Generador y Auditor de Contraseñas (`PasswordSecurityEngine` + `zxcvbn`)
- **Entropía Criptográfica + Auditoría de Indescifrabilidad:** Combina `java.security.SecureRandom` con la librería de análisis heurístico y de entropía `com.nulab-inc:zxcvbn`. Durante la generación, realiza bucles de verificación activa hasta asegurar un score de 4/4 (indescifrable por fuerza bruta y diccionarios).
- **3 Tamaños configurables:** 16 caracteres (Alta), 24 caracteres (Muy Alta) y 32 caracteres (Ultra Segura).
- **Aviso Nativo In-App (No Toasts):** Cuando el usuario teclea su propia contraseña, el motor la audita al instante. Si se detecta un score bajo o patrones vulnerables, la interfaz despliega una tarjeta de aviso nativo explicando el riesgo y el tiempo estimado de descifrado, sin interrumpir ni bloquear al usuario de realizar su firma si así lo desea.
- **Compatibilidad con scripts:** Se excluyen caracteres ambiguos o conflictivos en scripts bash/gradle como comillas dobles, comillas simples, backticks o barras invertidas (`"`, `'`, `` ` ``, `\`), asegurando que las contraseñas generadas puedan ser usadas sin escapar en `signingConfigs` de Gradle y en comandos de `apksigner`.

### 8. Compresión Nativa y Exportación Granular (Storage Access Framework + ZIP All-in-One)
- **Compresión Máxima sin Pérdidas:** `StorageCompressionHelper` utiliza `Deflater(Deflater.BEST_COMPRESSION)` nivel 9 y `GZIPOutputStream` con búferes de 8 KB estándar en el JDK de Android.
  - Para artefactos de texto (Base64, Gradle, YAML, info), la tasa de compresión supera frecuentemente el 60-70%.
  - Para el archivo binario del almacén PKCS12 / JKS, reduce cualquier redundancia interna sin alterar ni un solo bit tras la descompresión.
- **Doble Vía de Salida (FileProvider vs SAF):**
  - **FileProvider:** Empleado cuando el usuario pulsa "Compartir" para despachar el archivo a aplicaciones externas (Telegram, WhatsApp, Drive, Gmail) mediante `Intent.ACTION_SEND` con permisos temporales de lectura.
  - **Storage Access Framework (SAF - `CreateDocument`):** Empleado cuando el usuario pulsa "Guardar en Móvil". Invoca el selector de almacenamiento nativo del sistema operativo Android, permitiéndole elegir libremente cualquier carpeta (Descargas, Documentos, tarjeta SD o carpetas de proyectos locales).
- **Exportación Granular:** Cada artefacto (almacén, Base64, Gradle, Workflow y certificados X.509) cuenta con su propio lanzador SAF para ser exportado individualmente, o en bloque integral dentro del `.zip`.

### 9. Restauración e Importación de Paquetes ZIP y Protección Anti Zip-Slip
- **Mitigación Crítica Zip-Slip:** Los archivos ZIP procesados provienen del almacenamiento externo o de fuentes no confiables. `ZipImportHelper` valida que el `canonicalPath` de cada archivo extraído comience estrictamente con el `canonicalPath` del directorio sandbox temporal (`context.cacheDir/temp_zip_imports/`). Si una entrada contiene secuencias maliciosas de escape (ej. `../../`), el flujo la rechaza inmediatamente arrojando `SecurityException`.
- **Heurística de Credenciales:** La app analiza en memoria si el paquete ZIP incluye `signingConfigs.gradle.kts` o `INFO_KEYSTORE.txt`. Mediante expresiones regulares seguras, extrae de forma automática `storePassword`, `keyPassword` y `keyAlias`, precargándolos en la interfaz para evitar tecleos complejos y propensos a error en pantallas móviles táctiles.
- **Reconstrucción desde Base64:** Si el paquete ZIP carece de un archivo binario `.jks` o `.keystore` pero contiene una clave codificada en `.base64`, el motor la decodifica de forma segura y reconstruye el almacén binario en memoria.
- **Validación Dual de Almacén (PKCS12 + JKS):** La validación de contraseñas intenta primero el estándar moderno `PKCS12` y, ante fallos, prueba el formato legado `JKS`, asegurando compatibilidad con almacenes antiguos exportados desde herramientas previas de PC.
- **Persistencia Aislada:** El almacén se copia a `context.filesDir/keystores/` con verificación de no-colisión de nombres (agregando sufijos incrementales si ya existe un archivo con el mismo nombre) y las credenciales se cifran inmediatamente con **AES-256-GCM** mediante `SecureCredentialsCipher` antes de guardarse en Room.

---

## 🎨 Directrices de UI, Tipografía y Edge-to-Edge

### 1. Tamaño de Letra Fijo (`fontScale = 1.0f`)
- Para evitar que la configuración de accesibilidad del sistema operativo Android del usuario deforme la interfaz, corte textos o desborde tarjetas con alta densidad técnica, la app fuerza un escalado de fuente estricto (`fontScale = 1.0f`) mediante `CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, fontScale = 1.0f))` en `MyApplicationTheme`.
- La densidad de pantalla (ppi) se mantiene intacta, garantizando nitidez y adaptabilidad en cualquier resolución de smartphone sin rupturas visuales.

### 2. Gestión Quirúrgica de Insets Edge-to-Edge (Sin Doble Espaciado Superior)
- El `Scaffold` principal de `MainActivity` acondiciona su `contentWindowInsets` y el padding del `NavHost`:
  - En pantallas principales (`Generator`, `KeystoreList`, `Settings`), aplica los insets del sistema para dar cabida a la barra superior y la barra inferior.
  - En pantallas secundarias/hijas (`KeystoreDetailScreen`, `ColorPickerScreen`), suprime los insets del contenedor raíz (`PaddingValues(0.dp)`) permitiendo que cada pantalla hija gobierne su propio `Scaffold` y `TopAppBar` nativamente contra la barra de estado, erradicando franjas vacías o duplicación de márgenes superiores.

### 3. Transiciones de Navegación y Micro-animaciones Nativas (Zero Overhead)
- **Animaciones Direccionales de Pestañas:** En `MainActivity.kt`, los métodos de transición del `NavHost` (`enterTransition`, `exitTransition`, `popEnterTransition`, `popExitTransition`) emplean `NavRoutes.getBottomBarOrder()` para discernir la orientación del desplazamiento según el índice de la pestaña (Generador = 0, Almacén = 1, Ajustes = 2). Si se navega de izquierda a derecha se desplaza hacia la izquierda, y viceversa, otorgando una sensación táctil natural.
- **Transiciones de Pantallas de Detalle:** El paso a `KeystoreDetailScreen` y `ColorPickerScreen` se anima con deslizamiento horizontal completo y fundido cruzado (`FastOutSlowInEasing`).
- **Micro-animaciones en Tarjetas:** Se integra `Modifier.animateContentSize()` en tarjetas dinámicas (`GeneratorScreen`, `KeystoreListScreen`, `KeystoreDetailScreen`, `ColorPickerScreen`), garantizando que la expansión de campos opcionales, la aparición de avisos de seguridad en vivo y las previsualizaciones de contraseñas ocurran con una transición suave y continua sin saltos bruscos.
- **Pestañas Internas en Bloques Extensos:** En `KeystoreDetailScreen`, los fragmentos de código (`build.gradle.kts` y `GitHub Actions CI/CD`) se presentan con un `SecondaryTabRow` animado con `AnimatedContent`. Esto ahorra desplazamiento vertical excesivo en pantallas táctiles de teléfonos móviles, permitiendo al usuario cambiar de entorno con un solo toque y copiar el código sin perder el contexto visual.
- **Cero Impacto en Tamaño de APK:** Todas las animaciones provienen de las APIs nativas de `androidx.compose.animation`, garantizando fluidez a 60/120 FPS sin añadir dependencias externas pesadas.

---

## 🚫 Restricciones de Licencias de Dependencias
- **Prohibición estricta:** No agregar dependencias con licencias GPL, AGPL o licencias que obliguen al proyecto a abrir su código fuente o exponerlo públicamente.
- Se permiten licencias MIT, Apache 2.0, BSD y Bouncy Castle.
- No utilizar soluciones "sin dependencias" hechas a mano si existe una librería estándar probada que garantice el 100% de compatibilidad.
