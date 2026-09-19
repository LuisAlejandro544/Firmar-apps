# Instrucciones y Reglas para Agentes (AGENTS.md) 📋

Este archivo contiene las directrices de trabajo, reglas de desarrollo y preferencias del usuario para cualquier agente de IA que interactúe con este repositorio.

---

## 👤 Perfil del Usuario y Entorno
- **Sin PC:** El usuario utiliza exclusivamente un teléfono móvil. Las interfaces y soluciones deben ser completamente operables desde una pantalla táctil.
- **Canal de distribución:** La app se distribuirá en plataformas como Uptodown o descargas directas de APK de terceros (no en Google Play Store).
- **Idioma:** Toda la comunicación, explicaciones, comentarios de código y archivos de control (como `commit_message.txt`) deben ser en **español**. Si existe `commit_message.txt`, no modificarlo salvo petición expresa del usuario.

---

## 🧠 Metodología de Trabajo y Razonamiento
- **Razonar antes de actuar:** No responder ni editar a ciegas. Analizar detalladamente las herramientas a utilizar, el impacto en la arquitectura y las consecuencias antes de aplicar cualquier cambio.
- **Sin atajos perjudiciales:** No tomar caminos fáciles o soluciones a medias que dejen cabos sueltos o errores en tiempo de ejecución. Seguir las especificaciones al pie de la letra.
- **Inspección selectiva:** No abrir ni leer archivos de código innecesarios que no tengan relación directa con la petición actual del usuario.

---

## 📦 Gestión de Dependencias y Licencias
- **Funcionalidad sobre peso de APK:** Al usuario no le preocupa el peso final del APK siempre que las librerías sean 100% confiables y estables. Evitar crear soluciones caseras frágiles sin dependencias cuando existan bibliotecas estándar consolidadas.
- **Licencias restrictivas prohibidas:** No utilizar ni recomendar librerías con licencias copyleft virulentas (GPL, AGPL) que obliguen a publicar el código fuente o que exijan créditos que comprometan el proyecto.
- **Propiedad intelectual:** Evitar nombrar marcas registradas o protegidas por derechos de autor en nombres de archivos o paquetes que puedan poner en riesgo al usuario.

---

## ⚙️ Desarrollo Técnico y Arquitectura
- **Desarrollo Modular:** Mantener una arquitectura modular por capas (datos, criptografía, UI, navegación) para evitar el colapso de la aplicación.
- **Soporte de Arquitecturas:** Garantizar compatibilidad plena con arquitecturas de **32 bits** (`armeabi-v7a`) y **64 bits** (`arm64-v8a`, `x86_64`).
- **Versión mínima de Android:** Considerar siempre el `minSdk` antes de incorporar nuevas APIs y evaluar cuidadosamente si vale la pena elevarlo.
- **Código autodocumentado:** Todos los archivos de código deben incluir comentarios explicativos en español sobre la lógica que contienen.
- **Inclusión nativa total:** Si en el futuro se utiliza C++, Rust o Python, deben estar completamente vinculados en el sistema de compilación de Gradle sin omitir tareas ni añadir funciones fallback no deseadas.
- **Restricción de rendimiento móvil:** En caso de interactuar con optimizadores o boosters del sistema, jamás utilizar propiedades `persist.sys.*`.

---

## 🎨 Diseño y Experiencia de Usuario (UI/UX)
- **No al minimalismo extremo:** Al usuario no le agrada el minimalismo vacío. La interfaz debe ser atractiva, con densidad de información equilibrada, tarjetas y detalles visuales de calidad.
- **Pantallas separadas y navegación clara:** No amontonar todas las funciones en una sola pantalla. Utilizar pantallas dedicadas para cada flujo (generación, listado, detalles) con botones de navegación claros e intuitivos.
- **Preservación del diseño existente:** En cada cambio, no alterar la estética, iconos ni temas visuales existentes a menos que el usuario lo solicite expresamente.
- **Preservación de la lógica funcional:** No modificar la lógica que ya funciona sin justificación, para evitar regresiones o fallos en cascada.
