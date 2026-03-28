# Plan Arquitectónico: Generador SCORM (JavaFX + Spring Boot)

Este plan detalla la construcción desde cero de la aplicación SCORM, abandonando el monolito acoplado pre-existente para adoptar un sistema profesional, mantenible y escalable basado netamente en **Clean Architecture**.

## User Review Required

> [!IMPORTANT]
> Verifica este patrón **Cliente-Servidor Local** propuesto. Tendremos dos componentes separados ejecutándose en tu misma máquina (desacoplados visualmente). ¿Estás de acuerdo con arrancar con esta estructura multimódulo?

## Proposed Architecture (Clean Architecture Multimódulo)

No vamos a armar un proyecto donde la vista y la lógica estén mezcladas. Obligaremos al código a respetar barreras mediante módulos:

### 1. Dominio (`scorm-core`)
El corazón de tu lógica de aprendizaje e e-learning. Sin frameworks externos.
- **Entidades:** Agregados propios del negocio: curso, capítulos, estructura general de metadatos, árbol SCORM.
- **Interfaces (Outbound Ports):** Contratos de todo lo que dependa de hardware externo (Bases de datos, Lector de archivos PDF, Motor generador de ZIP).

### 2. Application (`scorm-application`)
El director de la orquesta.
- **Use Cases:** Toda la inteligencia de nuestra app. `ExtractTextAndImagesFromPdfUseCase`, `CreateScormPackageUseCase`. 

### 3. Frameworks & Drivers / Infraestructura (`scorm-infrastructure`)
El bajo nivel. Todo aquello de lo que tu dominio se protege.
- **Lector de PDF (PDFBox Adapter):** Lee de manera binaria el PDF subido mediante *Apache PDFBox*, procesando páginas e imágenes a estructuras puramente de Dominio.
- **Template Engine (Thymeleaf/FreeMarker Adapter):** Convertimos nuestro domino en HTML inyectando datos a *templates* limpias, responsivas y bonitas que se empaquetarán luego.
- **Generador SCORM:** Orquesta la creación del estricto estándar SCORM (XML `imsmanifest` y la construcción del ZIP).

### 4. Presentación Backend (`scorm-api`)
- **Adaptadores de Entrada (REST):** Tu API web local montada en **Spring Boot**. Recibe de la interfaz los recursos y expone los Casos de Uso del core en la ruta `http://localhost:8080/api/v1/...`.

### 5. Presentación Frontend (`scorm-javafx`)
- **Cliente (Desktop UI):** Una aplicación de escritorio 100% tonta pero hermosa. Única responsabilidad: interactuar con el usuario y pedirle cosas al Backend local mediante HTTP. Esto asegura la escalabilidad futura total.

## Verification Plan

### Automated Tests
- Al seguir Clean Architecture probaremos los `Use Cases` inyectando dependencias falsas (mockeadas) para testear que la lógica de parsear capítulos no depende nunca de Spring Boot.

### Manual Verification
1. Compilar todo el ecosistema localmente.
2. Iniciar el servicio backend Spring (`localhost:8080` de fondo).
3. Iniciar el Frontend cliente JavaFX.
4. Generar arrastre de validación de un archivo en formato "Libro.pdf".
5. Extraer el `.zip` resultante y validarlo localmente en la web de pruebas "SCORM Cloud" para chequear pureza estructural del manifiesto.
