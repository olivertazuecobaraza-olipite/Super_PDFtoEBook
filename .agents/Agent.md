# Orquestador Principal: SCORM Generator App

## Contexto del Proyecto
Este proyecto reemplaza una antigua aplicación monolítica por una herramienta profesional, robusta y escalable guiada por los principios de **Clean Architecture**.
- **Frontend:** App de escritorio pura, diseñada en JavaFX.
- **Backend:** API REST local mediante Spring Boot (`localhost:8080`).
- **Core:** Lógica de negocio y procesamiento de PDFs nativo en Java.

## Reglas de Delegación (Atención IA)
**NUNCA operes asumiendo todo el sistema a la vez.** Un prompt gigante arruina la calidad del código. Regla estricta: Antes de programar una tarea, **lee el fichero de rol correspondiente a la capa en la que vas a trabajar**:

1. ¿Vas a trabajar en Entidades, Lógica Pura o Casos de Uso (Core)? 
   👉 Carga **`.agents/role-domain.md`**
2. ¿Vas a programar Controladores REST, Seguridad o Inyección de Dependencias (Spring)? 
   👉 Carga **`.agents/role-spring.md`**
3. ¿Vas a diseñar Interfaces Gráficas con FXML o lógica visual de escritorio? 
   👉 Carga **`.agents/role-javafx.md`**
4. ¿Vas a tocar los algoritmos pesados de PDFBox, compresión ZIP o renderizado Thymeleaf? 
   👉 Carga **`.agents/role-parser.md`**
5. ¿Vas a implementar o retocar el diseño visual basado en el sistema de Stitch?
   👉 Carga **`.agents/role-design.md`**

Si la tarea involucra a más de una capa, carga los roles uno por uno y aplica los cambios en orden (generalmente de adentro hacia afuera: Dominio -> Infra -> API -> Vista).
