package com.oliver.infrastructure.config;

import javafx.fxml.FXMLLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

/**
 * El pegamento maestro entre JavaFX y Spring Boot.
 * Carga las vistas FXML pero engaña a JavaFX para que pida los
 * Controladores a Spring Boot en lugar de instanciarlos solo.
 */
@Component
public class SpringFXMLLoader {

    private final ApplicationContext context;

    public SpringFXMLLoader(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Carga un archivo FXML y devuelve el loader configurado con el factory de Spring.
     * @param fxmlPath Ruta absoluta del root del classpath (ej: "/views/MainView.fxml")
     */
    public FXMLLoader getLoader(String fxmlPath) throws IOException {
        URL url = getClass().getResource(fxmlPath);
        if (url == null) {
            throw new IOException("Falta un cimiento: No se encontró el archivo FXML en -> " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(url);
        // ACÁ OCURRE LA MAGIA DE LA INYECCIÓN
        // JavaFX intentará crear el controlador (clase en el fx:controller)... 
        // Nosotros lo bypasseamos y le decimos: "Pedíselo a Spring Boot que lo tiene configurado"
        loader.setControllerFactory(context::getBean); 
        
        return loader;
    }
}
