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
app/src/main/
├── AndroidManifest.xml                  # Declaración de actividades y FileProvider
├── java/com/example/
│   ├── MainActivity.kt                  # Actividad principal y Scaffold con NavHost
│   │
│   ├── crypto/                          # Motor Criptográfico y Utilidades
│   │   ├── KeystoreGenerator.kt         # Generador de pares de claves RSA y certificados X.509
│   │   └── KeystoreExportHelper.kt      # Compartición mediante FileProvider, copias y snippets
│   │
│   ├── data/                            # Capa de Persistencia y Modelos
│   │   ├── dao/
│   │   │   └── KeystoreDao.kt           # Consultas SQL con Room (CRUD y Flow reactivo)
│   │   ├── database/
│   │   │   └── AppDatabase.kt           # Instancia singleton de la base de datos Room
│   │   ├── model/
│   │   │   └── KeystoreEntity.kt        # Entidad de tabla con credenciales y metadatos
│   │   └── repository/
│   │       └── KeystoreRepository.kt    # Unifica base de datos con limpieza en el sistema de archivos
│   │
│   └── ui/                              # Capa de Presentación (Jetpack Compose)
│       ├── detail/
│       │   ├── KeystoreDetailScreen.kt  # Vista de detalle, credenciales, huellas y exportación
│       │   └── KeystoreDetailViewModel.kt # Lógica de revelación de contraseñas y eliminación
│       ├── generator/
│       │   ├── GeneratorScreen.kt       # Formulario interactivo para crear nuevas keystores
│       │   └── GeneratorViewModel.kt    # Validación de entradas y coordinación en segundo plano
│       ├── list/
│       │   ├── KeystoreListScreen.kt    # Listado con buscador, tarjetas informativas y estado vacío
│       │   └── KeystoreListViewModel.kt # Filtrado en tiempo real y confirmación de borrado
│       ├── navigation/
│       │   └── NavRoutes.kt             # Definición tipada de rutas y destinos de barra inferior
│       └── theme/
│           ├── Color.kt                 # Paleta de colores índigo/cian de seguridad
│           ├── Theme.kt                 # Tema Material Design 3 dinámico
│           └── Type.kt                  # Tipografía de la aplicación
│
└── res/
    ├── drawable/                        # Vectores del icono del launcher y fondos
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
* **`KeystoreGenerator.kt`**: Implementa la lógica criptográfica sin depender de `keytool` de PC. Crea claves RSA de 2048/4096 bits, emite certificados X.509 v3 usando el proveedor nativo Conscrypt/Bouncy Castle, calcula huellas SHA-1 y SHA-256 en formato hexadecimal y guarda el archivo `.jks` en el almacenamiento interno de la app (`filesDir/keystores/`).
* **`KeystoreExportHelper.kt`**: Proporciona métodos para compartir de forma segura el archivo físico con otras aplicaciones (vía `FileProvider` con permisos `FLAG_GRANT_READ_URI_PERMISSION`), copiar textos al portapapeles y generar el bloque de configuración `signingConfigs` para `build.gradle.kts`.

### 2. `data/`
* **`KeystoreEntity.kt`**: Modela los datos de la keystore: ID, título, nombre de archivo, ruta absoluta, alias, contraseñas, algoritmo, tamaño en bytes, validez en años y huellas.
* **`KeystoreDao.kt`**: Consultas Room para inserción, lectura ordenada por fecha descendente, búsqueda por ID y borrado.
* **`KeystoreRepository.kt`**: Repositorio que sincroniza Room con el disco: al borrar un registro, elimina también el archivo físico para no saturar la memoria del teléfono.

### 3. `ui/`
* **`GeneratorScreen.kt` & `GeneratorViewModel.kt`**: Pantalla de creación asistida con datos de prueba, visibilidad de contraseñas y validaciones.
* **`KeystoreListScreen.kt` & `KeystoreListViewModel.kt`**: Lista de keystores con buscador en vivo y tarjeta con metadata esencial.
* **`KeystoreDetailScreen.kt` & `KeystoreDetailViewModel.kt`**: Visualización completa de credenciales, copiado de huellas y botón de exportación.
