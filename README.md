# Super PDF to eBook 🚀

¡Bienvenido al generador de eBooks SCORM. Este proyecto no es solo un conversor; es una solución de **Arquitectura Hexagonal** diseñada para transformar PDFs educativos en experiencias interactivas de alto rendimiento compatibles con cualquier plataforma LMS (Moodle, Canvas, Blackboard, etc.).

---

## 🏗️ Arquitectura del Sistema: Hexagonal

El sistema está construido bajo los principios de **Arquitectura Hexagonal**, lo que garantiza un desacoplamiento total entre la lógica de negocio y las herramientas externas (como bibliotecas de PDF o generadores de ZIP).

### Organización de Carpetas (Source Structure)

```text
src/main/java/com/oliver/
│
├── core/                       # 🧠 EL CEREBRO (Domain & Application)
│   ├── application/            # Casos de uso y Orquestación
│   │   ├── ports/              # Interfaces (Input/Output Ports)
│   │   └── usecases/           # Implementación de la lógica de negocio
│   └── domain/                 # Modelos de dominio puros (sin dependencias)
│       └── models/             # EbookPagesMap, Page data, etc.
│
├── infrastructure/             # 🛠️ LAS HERRAMIENTAS (Adapters)
│   ├── adapters/               # Implementaciones externas
│   │   ├── out/                # Apache PDFBox, ZipGenerator, etc.
│   │   └── in/                 # (Si hubiera adaptadores de entrada externos)
│   └── config/                 # Configuración de Spring Boot & Beans
│
└── presentation/               # 🖥️ LA CARA (UI Layer)
    └── application/            # JavaFX Application & Controllers
```

---

## 🌟 Características Estelares

### 1. Renderizado a Disco Físico (RAM-Safe)
A diferencia de otros conversores que colapsan la memoria usando Base64, nuestro sistema rasteriza cada página del PDF a imágenes `.jpg` físicas en un workspace temporal. Esto permite procesar libros de cientos de páginas sin despeinarse.

### 2. Visor Interactivo Premium (SCORM)
El paquete generado incluye un visor web basado en HTML5/JS con:
*   **Modo Libro Abierto**: Vista de doble página (izquierda/derecha).
*   **Zoom Dinámico**: Control de escalado suave.
*   **Responsive**: Adaptado para diferentes resoluciones.
*   **Offline-First**: Funciona sin internet una vez descargado.

### 3. AudioLectura (Text-To-Speech) Integrada
Extraemos el texto vivo de cada página y lo inyectamos en una "base de datos" JS estática dentro del SCORM. El usuario puede activar la narración en voz alta con un click usando la **Web Speech API** del navegador.

### 4. Sistema de Índice de Triple Jerarquía
El visor ofrece un panel lateral (Sidebar) interactivo con tres niveles de prioridad:
1.  **Manual**: El usuario puede pegar un índice custom (`Título - Página`) en el escritorio.
2.  **Nativo**: Si no hay manual, extrae automáticamente el *Outline* (Bookmarks) del PDF.
3.  **Fallback**: Genera saltos rápidos automáticos (cada 10 páginas) si el PDF no tiene metadatos.

---

## 🛠️ Stack Tecnológico

*   **Backend**: Java 17 + Spring Boot 3.
*   **UI Escritorio**: JavaFX + FXML (con arquitectura hexagonal inyectada).
*   **PDF Engine**: Apache PDFBox (PDFRenderer & TextStripper).
*   **Frontend del Visor**: Vanilla HTML5, CSS Premium (Gradients & Micro-animations), JavaScript.
*   **Emu**: SCORM 1.2 compliant.

---

## 🚀 Cómo Empezar

### Instalación Directa (Recomendado)
1.  Ve a la sección de **Releases** de este repositorio de GitHub.
2.  Descarga la versión más reciente del instalador (`SuperPDF-Installer.exe` o el archivo `.zip` ejecutable).
3.  Ejecuta el instalador y sigue las instrucciones en pantalla.

### Para Desarrolladores (Build desde Código)
Si prefieres compilarlo tú mismo:
1.  Asegúrate de tener **JDK 17+** y **Maven** instalados.
2.  Clona el repositorio: `git clone https://github.com/olivertazuecobaraza-olipite/Super_PDFtoEBook.git`
3.  Ejecuta `mvn clean install`.
4.  Lanza la aplicación con `mvn spring-boot:run`.

### Uso
1.  Selecciona tu archivo PDF.
2.  (Opcional) Pega el índice personalizado en el área de texto, o se creara un índice automático basado en los marcadores del PDF.
3.  Dale a **Generar SCORM**.
4.  ¡Listo! Encontrarás tu ZIP interactivo en el escritorio o ruta configurada.

---

## 📜 Licencia & Créditos

Desarrollado con pasión y arquitectura de hierro por **Oliver**.