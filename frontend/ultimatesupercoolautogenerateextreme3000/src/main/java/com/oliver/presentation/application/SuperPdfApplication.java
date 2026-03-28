package com.oliver.presentation.application;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada principal para Spring Boot.
 * Desde acá delegamos el arranque a la clase responsable de JavaFX.
 */
@SpringBootApplication(scanBasePackages = "com.oliver")
public class SuperPdfApplication {

    public static void main(String[] args) {
        // Delegamos el inicio a JavaFX
        Application.launch(JavaFxApplication.class, args);
    }
}
