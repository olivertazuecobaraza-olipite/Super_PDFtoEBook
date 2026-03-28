package com.oliver.presentation.controllers;

import com.oliver.infrastructure.config.SpringFXMLLoader;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Controlador principal y Master Router (App Shell).
 * Mantiene el SideBar y el Header vivos, y cambia el centro.
 */
@Component
public class MainController {

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnConverter;

    @FXML
    private Button btnLibrary;

    private final SpringFXMLLoader springLoader;

    // Spring Boot 3+ inyecta automáticamente si hay un solo constructor, no hace falta @Autowired
    public MainController(SpringFXMLLoader springLoader) {
        this.springLoader = springLoader;
    }

    @FXML
    public void initialize() {
        System.out.println("✅ MainController (Enrutador Maestro) inyectado.");
        loadPage("/views/ConverterView.fxml");
    }

    @FXML
    public void goToConverter() {
        loadPage("/views/ConverterView.fxml");
        setActiveButton(btnConverter);
    }

    @FXML
    public void goToLibrary() {
        loadPage("/views/LibraryView.fxml");
        setActiveButton(btnLibrary);
    }

    /**
     * Reemplaza el contenido central por la nueva vista FXML cargada por Spring.
     */
    public void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = springLoader.getLoader(fxmlPath);
            Node view = loader.load();
            this.contentArea.getChildren().setAll(view);
            System.out.println("🔄 Navegando a: " + fxmlPath);
        } catch (IOException e) {
            System.err.println("❌ ERROR INYECTANDO VISTA SECUNDARIA: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Gestiona las clases CSS para pintar qué menú está activo
     */
    private void setActiveButton(Button activeButton) {
        // Limpiamos la clase activa de todos
        btnConverter.getStyleClass().remove("menu-item-active");
        btnLibrary.getStyleClass().remove("menu-item-active");
        
        // Se la ponemos solo al que tocamos
        if (!activeButton.getStyleClass().contains("menu-item-active")) {
            activeButton.getStyleClass().add("menu-item-active");
        }
    }
}
