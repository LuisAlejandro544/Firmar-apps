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
- [x] **Distribución de Artefactos Individuales por Arquitectura:**
  - [x] `APK-Debug-arm64-v8a`: Binario específico para procesadores ARM de 64 bits.
  - [x] `APK-Debug-armeabi-v7a`: Binario específico para procesadores ARM de 32 bits.
  - [x] `APK-Debug-x86_64`: Binario específico para emuladores y PC.
  - [x] `APK-Debug-Universal`: Binario unificado compatible con todas las arquitecturas.
  - [x] Subida en artifacts independientes para descarga directa desde el móvil sin requerir descomprimir zips complejos.

---

### ⏳ Fase 6: Importador e Inspector de Keystores Externas (Próximo Sprint)
- [ ] Selector de archivos para importar keystores existentes desde el almacenamiento del teléfono.
- [ ] Extracción y visualización de certificados, alias y huellas de archivos externos.
- [ ] Verificación de contraseñas de almacén y de clave para keystores importadas.

---

### 🔮 Fase 7: Firma Móvil de APKs
- [ ] Herramienta para seleccionar un APK no firmado en el dispositivo y firmarlo con una de las keystores guardadas mediante v1, v2 y v3 scheme.
- [ ] Alineación con `zipalign` integrada.
