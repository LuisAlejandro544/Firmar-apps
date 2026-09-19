# Keystore Creator 🔐

Una aplicación móvil moderna desarrollada en **Kotlin** y **Jetpack Compose** para generar, gestionar y exportar archivos Keystore (`.jks` / `.keystore` en formato estándar PKCS12) directamente desde un teléfono Android, sin necesidad de una computadora ni herramientas de terminal como `keytool`.

---

## 📱 Características Principales

- **Generador de Keystores Criptográfico:**
  - Creación de pares de claves RSA (2048 y 4096 bits) con certificados X.509 v3 autofirmados.
  - Firma digital con algoritmo `SHA256withRSA` utilizando el proveedor de seguridad nativo de Android.
  - Configuración de validez (25, 30, 50 años) y campos opcionales del certificado (CN, OU, O, C).
  - Opción de "Misma contraseña para la clave" para mayor comodidad al escribir en el teléfono.
  - Botón de **Datos de prueba** para validaciones instantáneas.

- **Gestión Local Segura (Room Database):**
  - Registro de todas las keystores creadas con sus credenciales (alias, contraseñas protegidas).
  - Visualización del tamaño del archivo, fecha de creación y algoritmo empleado.
  - Buscador rápido en tiempo real por título, nombre de archivo o alias.
  - Eliminación segura que limpia tanto la base de datos como el archivo físico del almacenamiento interno.

- **Detalles y Exportación para Firma de APKs:**
  - Cálculo automático y copia en 1 toque de huellas digitales **SHA-256** y **SHA-1** (vitales para Firebase, Google Sign-In, Play Console, etc.).
  - Generador de bloques de configuración listos para `build.gradle.kts` y comandos de `apksigner`.
  - Exportación y compartición directa del archivo `.jks` mediante `FileProvider` a herramientas móviles como MT Manager, Google Drive, WhatsApp, Telegram o el gestor de archivos del teléfono.

---

## 🛠️ Stack Tecnológico

| Componente | Tecnología |
|---|---|
| **Lenguaje** | Kotlin 2.2 |
| **Interfaz de Usuario** | Jetpack Compose (Material 3) |
| **Arquitectura** | MVVM (Model-View-ViewModel) con Flow reactivo |
| **Base de Datos Local** | AndroidX Room con KSP |
| **Criptografía** | Bouncy Castle (`bcprov-jdk18on`, `bcpkix-jdk18on`) + Conscrypt Android |
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
