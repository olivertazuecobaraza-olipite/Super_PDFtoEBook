# 🏗️ Super PDF-to-SCORM: Arquitectura y Funcionamiento Interno

¡Escuchame bien, hermano! No estamos haciendo un script de 10 líneas. Estamos ante una **Arquitectura Hexagonal (Ports & Adapters)** hecha y derecha. Si querés entender cómo este bicho convierte un PDF rancio en un paquete SCORM interactivo que vuela, sentate y prestá atención.

---

## 🏛️ 1. El Diseño: Arquitectura Hexagonal
Acá no mezclamos las papas con el motor. El proyecto está dividido en tres capas claras:

1.  **`com.oliver.core` (El Cerebro/Dominio)**:
    - No sabe qué es un PDF, ni qué es Windows o Linux.
    - Define **Puertos (Interfaces)**: "Necesito alguien que me extraiga hojas de un PDF" (`PdfExtractorPort`).
    - Contiene el **Caso de Uso** (`ConvertPdfToScormUseCaseImpl`): Es el director de orquesta. Él dice qué se hace y cuándo, pero no *cómo* se hace técnicamente.

2.  **`com.oliver.infrastructure` (Los Músculos/Adapter)**:
    - Acá es donde vive la tecnología real.
    - **`ApachePdfExtractorAdapter`**: El tipo que se pelea con los archivos PDF usando Apache PDFBox.
    - **`ZipScormGeneratorAdapter`**: El artesano que construye el ZIP siguiendo el estándar SCORM 1.2.
    - **`CsvLibraryRepositoryAdapter`**: El bibliotecario que anota todo en un CSV para que la App no olvide lo que hizo.

3.  **`com.oliver.presentation` (La Cara/UI)**:
    - JavaFX. Solo se encarga de que el usuario vea botones lindos y barritas de progreso.

---

## 🚀 2. El Mecanismo de Conversión (Step-by-Step)

Cuando le das al botón de "Convertir", esto es lo que pasa bajo el capó:

### Fase A: La Extracción Estructural (`ApachePdfExtractorAdapter`)
Es la parte más pesada (50% del proceso). No solo "copiamos" el PDF, lo **deconstruimos**:
1.  **Renderizado Crudo**: Usamos PDFBox para convertir cada página en una imagen JPG de alta calidad (200 DPI). ¿Por qué JPG? Porque los visores SCORM en navegadores web manejan mejor imágenes estáticas que PDFs embebidos pesados.
2.  **Minería de Datos (Texto/TTS)**: Por cada página, extraemos el texto plano. Este texto se guarda en archivos `.txt` temporales. Esto es lo que permite que el visor final tenga función de "Lectura en voz alta" (TTS) sin internet.
3.  **Inteligencia de Índice (TOC)**: El sistema busca un índice (Marcadores) de tres formas en este orden de prioridad:
    - **Manual**: Lo que se escriba en el cuadro de texto.
    - **Nativo**: Los marcadores que ya traía el PDF de fábrica.
    - **Failsafe**: Si no hay nada, el sistema inventa un índice de paginación básica para que el alumno no se pierda.

### Fase B: El Ensamblaje SCORM (`ZipScormGeneratorAdapter`)
Acá es donde ocurre la magia del empaquetado:
1.  **Inyección de Plantillas**: Tenemos plantillas HTML/CSS/JS "escondidas" en los recursos. El sistema las lee y "limpia" los placeholders (ej: cambia `[[TOTAL_PAGES]]` por el número real).
2.  **El Truco del `texts.js` (Bypass CORS)**: Para que el navegador pueda leer el texto de las páginas sin que salten errores de seguridad (CORS) cuando se abre localmente, metemos todo el texto extraído en un archivo JavaScript (`assets/js/texts.js`) como un array gigante. ¡Locura cósmica!
3.  **El Manifiesto (`imsmanifest.xml`)**: Generamos el archivo XML sagrado que Moodle o cualquier LMS lee para saber que esto es un curso SCORM válido.
4.  **Compresión ZIP**: Metemos las imágenes, el JS, el CSS y el manifiesto en un ZIP.

### Fase C: Finalización y Limpieza
1.  **Registro**: Se guarda el ID y la ruta del ZIP en `library.csv`.
2.  **Barrido de Casa**: El bloque `finally` del Caso de Uso borra todos los JPGs y TXTs temporales del disco duro. **No dejamos basura**, somos profesionales.

---

## 🛠️ Tecnologías Clave
- **Apache PDFBox**: Para renderizar y extraer texto.
- **JavaFX**: Para la interfaz de escritorio.
- **Spring Boot (Core)**: Para la inyección de dependencias (para que todo esté desacoplado).
- **SCORM 1.2**: El estándar de E-learning.

¿Se entiende, loco? Es una máquina bien aceitada. No es solo "mover archivos", es transformar un documento estático en una aplicación web interactiva y autocontenida.

**¡Dale gas!** 🚀
