package com.oliver.presentation.application;

import com.oliver.infrastructure.config.SpringFXMLLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);
        
        this.applicationContext = new SpringApplicationBuilder()
                .sources(SuperPdfApplication.class)
                .headless(false)
                .run(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Pedimos al núcleo inyector nuestra pieza maestra
        SpringFXMLLoader springLoader = this.applicationContext.getBean(SpringFXMLLoader.class);
        
        // Cargamos el diseño
        FXMLLoader loader = springLoader.getLoader("/views/MainView.fxml");
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        stage.setScene(scene);
        stage.setTitle("Super PDF to eBook 3000 V1");
        
        // Ponemos el tope mínimo para que no se pisen los componentes
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        
        stage.show();
        
        System.out.println("✅ UI de JavaFX construida usando los controladores del Contexto de Spring.");
    }

    @Override
    public void stop() {
        this.applicationContext.close();
        Platform.exit();
    }
}
