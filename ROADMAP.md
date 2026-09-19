# Roadmap del Proyecto: Keystore Creator 🗺️

Este documento define el plan de evolución y las próximas funcionalidades de la aplicación.

---

## 🎯 Próximo Paso Prioritario (Siguiente Implementación)

### 📌 Conversión de Keystore a Formato Base64 (`.base64` / String)
* **Objetivo:** Permitir convertir cualquier archivo `.jks` generado o existente a una cadena codificada en **Base64** directamente desde la pantalla de detalle de la keystore.
* **Casos de uso clave:**
  - **CI/CD:** Facilitar la integración con GitHub Actions (`ANDROID_KEYSTORE_BASE64`), GitLab CI o Bitrise sin tener que transferir el archivo binario a una PC.
  - **Portabilidad móvil:** Copiar la cadena Base64 completa con un solo toque al portapapeles para guardarla en gestores de contraseñas o notas seguras.
  - **Exportación de archivo de texto:** Opción de compartir tanto la cadena de texto como un archivo `.txt` / `.base64` adjunto.
  - **Decodificación inversa:** Posibilidad futura de reconstruir el archivo `.jks` a partir de un texto Base64 pegado por el usuario.

---

## 📅 Estado de las Fases

### ✅ Fase 1: Fundamentos y Validación (Completada)
- [x] Motor criptográfico para generación de claves RSA (2048 y 4096 bits).
- [x] Construcción y firma de certificados X.509 autofirmados con validez configurable.
- [x] Empaquetado en formato estándar PKCS12 compatible con `.jks` y `.keystore`.
- [x] Base de datos local Room para persistencia offline segura de credenciales.
- [x] Navegación cómoda con `NavigationBar` y pantalla de detalle individual.
- [x] Extracción automática de huellas digitales SHA-256 y SHA-1.
- [x] Generador de fragmento de código de firma para `build.gradle.kts`.
- [x] Exportación y compartición de archivos vía Android `FileProvider`.

---

### ⏳ Fase 2: Conversión a Base64 y Utilidades CI/CD (Próximo Sprint)
- [ ] Botón de "Convertir a Base64" en la pantalla de detalle.
- [ ] Visor de texto Base64 con botón rápido de copia al portapapeles.
- [ ] Exportación directa de archivo `.base64` a través del selector de apps del sistema.
- [ ] Generador de plantilla para archivo de secretos de GitHub Actions (`.yml`).

---

### 🔮 Fase 3: Importador e Inspector de Keystores Externas
- [ ] Selector de archivos para importar keystores existentes desde el almacenamiento del teléfono.
- [ ] Extracción y visualización de certificados, alias y huellas de archivos externos.
- [ ] Verificación de contraseñas de almacén y de clave para keystores importadas.

---

### 🔮 Fase 4: Firma Móvil de APKs
- [ ] Herramienta para seleccionar un APK no firmado en el dispositivo y firmarlo con una de las keystores guardadas mediante v1, v2 y v3 scheme.
- [ ] Alineación con `zipalign` integrada.
