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

### 1. Formato PKCS12 vs JKS Clásico
- En versiones modernas de Android y Java (JDK 9+), el formato predeterminado y recomendado por Google para `apksigner` es **PKCS12** (con extensión `.jks` o `.keystore`).
- Bouncy Castle y el runtime de Android manejan PKCS12 con soporte completo para almacenar la clave privada RSA y la cadena de certificados X.509 v3.

### 2. Conflicto de Proveedores de Seguridad (`BC` vs Conscrypt)
- **Lección aprendida / Bug resuelto:** Android incluye internamente una implementación antigua y mutilada de Bouncy Castle bajo el nombre de proveedor `"BC"`.
- **Regla obligatoria:** Nunca forzar `.setProvider(BouncyCastleProvider.PROVIDER_NAME)` ni `.setProvider("BC")` para constructores de firma (`JcaContentSignerBuilder`) o conversores de certificados (`JcaX509CertificateConverter`). Debe permitirse al sistema utilizar su proveedor nativo (`Conscrypt`/`OpenSSL`) o pasar la instancia directa en memoria (`BouncyCastleProvider()`) como respaldo.

### 3. Almacenamiento y Compartición de Archivos (Zero Permissions)
- Las keystores generadas se guardan en el almacenamiento interno privado de la aplicación: `context.filesDir/keystores/`.
- No solicitar permisos peligrosos como `READ_EXTERNAL_STORAGE` o `WRITE_EXTERNAL_STORAGE`.
- La exportación hacia otras aplicaciones (gestores de archivos, mensajería, nube) se realiza mediante **`androidx.core.content.FileProvider`** con el URI de contenido `content://com.example.fileprovider/...`.

---

## 🚫 Restricciones de Licencias de Dependencias
- **Prohibición estricta:** No agregar dependencias con licencias GPL, AGPL o licencias que obliguen al proyecto a abrir su código fuente o exponerlo públicamente.
- Se permiten licencias MIT, Apache 2.0, BSD y Bouncy Castle.
- No utilizar soluciones "sin dependencias" hechas a mano si existe una librería estándar probada que garantice el 100% de compatibilidad.
