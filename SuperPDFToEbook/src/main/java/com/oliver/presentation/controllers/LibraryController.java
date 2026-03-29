package com.oliver.presentation.controllers;

import com.oliver.core.application.ports.out.LibraryRepositoryPort;
import com.oliver.core.domain.models.Ebook;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javafx.stage.FileChooser;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

@Component
public class LibraryController {

    private final LibraryRepositoryPort libraryRepository;

    @FXML
    private VBox booksContainer;

    private static File lastKnownDirectory = null;

    public LibraryController(LibraryRepositoryPort libraryRepository) {
        this.libraryRepository = libraryRepository;
    }

    @FXML
    public void initialize() {
        System.out.println("✅ LibraryController inyectado: Cargando base de datos...");
        loadBooksFromDatabase();
    }

    private void loadBooksFromDatabase() {
        try {
            // Ir al Core a recuperar la lista
            List<Ebook> books = libraryRepository.findAll();

            // Limpiar la grilla por las dudas
            booksContainer.getChildren().clear();

            if (books == null || books.isEmpty()) {
                showEmptyState();
                return;
            }

            // Renderizar las tarjetas de JavaFX por cada item
            for (Ebook book : books) {
                HBox card = createBookCard(book);
                booksContainer.getChildren().add(card);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al leer la base de datos de libros: " + e.getMessage());
            showEmptyState();
        }
    }

    private void showEmptyState() {
        HBox emptyState = new HBox(15);
        emptyState.getStyleClass().add("history-item");
        emptyState.setStyle("-fx-padding: 15px;");
        emptyState.setAlignment(Pos.CENTER_LEFT);

        Region icon = new Region();
        icon.setStyle(
                "-fx-background-color: #dde9ff; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8px;");

        VBox textContainer = new VBox(2);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Tu biblioteca está vacía");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #0d1c2f;");
        Label desc = new Label("Aún no hay ningún eBook disponible en tu colección local.");
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #515f74;");

        textContainer.getChildren().addAll(title, desc);
        emptyState.getChildren().addAll(icon, textContainer);

        booksContainer.getChildren().setAll(emptyState);
    }

    private HBox createBookCard(Ebook book) {
        HBox card = new HBox(15);
        card.getStyleClass().add("history-item");
        card.setStyle("-fx-padding: 15px;");
        card.setAlignment(Pos.CENTER_LEFT);

        Region icon = new Region();
        icon.setStyle(
                "-fx-background-color: #4CAF50; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8px;"); // Verde
                                                                                                                          // éxito

        VBox texts = new VBox(2);
        texts.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(book.title());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0d1c2f; -fx-font-size: 14px;");

        Label dateLbl = new Label("Generado el: " + book.creationDate().replace("T", " "));
        dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #515f74;");

        texts.getChildren().addAll(titleLbl, dateLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Botón de descargar a donde el usuario quiera
        Button btnDownload = new Button("📥 Descargar");
        btnDownload.setStyle(
                "-fx-background-color: #000666; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        btnDownload.setOnAction(e -> descargarArchivo(book));

        // Botón de eliminar (borra archivo, borra de DB e interfaz)
        Button btnDelete = new Button("🗑️ Eliminar");
        btnDelete.setStyle(
                "-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> eliminarLibro(book));

        card.getChildren().addAll(icon, texts, spacer, btnDownload, btnDelete);

        return card;
    }

    private void descargarArchivo(Ebook book) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Paquete SCORM");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo ZIP", "*.zip"));
        fileChooser.setInitialFileName(book.title().replace(" ", "_") + ".zip");

        if (lastKnownDirectory != null && lastKnownDirectory.exists()) {
            fileChooser.setInitialDirectory(lastKnownDirectory);
        }

        File selectedFile = fileChooser.showSaveDialog(booksContainer.getScene().getWindow());

        if (selectedFile != null) {
            lastKnownDirectory = selectedFile.getParentFile();
            
            Alert loadingAlert = new Alert(AlertType.INFORMATION);
            loadingAlert.setTitle("Guardando");
            loadingAlert.setHeaderText("Copiando paquete SCORM...");
            loadingAlert.setContentText("Por favor espera, no cierres la aplicación.");
            loadingAlert.getButtonTypes().clear(); // Quitar el botón OK para modo "Cargando"
            loadingAlert.show();
            
            javafx.concurrent.Task<Void> copyTask = new javafx.concurrent.Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    File internalFile = new File(book.filePath());
                    if (internalFile.exists()) {
                        Files.copy(internalFile.toPath(), selectedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        throw new Exception("El archivo físico no se encuentra. Quizás fue borrado.");
                    }
                    return null;
                }
            };
            
            copyTask.setOnSucceeded(e -> {
                loadingAlert.setResult(ButtonType.OK);
                loadingAlert.close();
                System.out.println("✅ SCORM descargado en: " + selectedFile.getAbsolutePath());
                
                Alert successAlert = new Alert(AlertType.INFORMATION);
                successAlert.setTitle("Éxito");
                successAlert.setHeaderText("¡Descarga Completada!");
                successAlert.setContentText("SCORM guardado correctamente.");
                successAlert.show();
            });
            
            copyTask.setOnFailed(e -> {
                loadingAlert.setResult(ButtonType.OK);
                loadingAlert.close();
                mostrarError("Error guardando el archivo: " + copyTask.getException().getMessage());
            });
            
            new Thread(copyTask).start();
        }
    }

    private void eliminarLibro(Ebook book) {
        Alert deleteAlert = new Alert(AlertType.CONFIRMATION);
        deleteAlert.setTitle("Confirmar eliminación");
        deleteAlert.setHeaderText("Vas a borrar el eBook '" + book.title() + "'");
        deleteAlert.setContentText("Esta acción es irreversible. ¿Proceder?");

        deleteAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    System.out.println("🗑️ Intentando borrar físico y luego DB: " + book.title());
                    // 1. Borrar del disco local (Físico primero)
                    File internalFile = new File(book.filePath());
                    if (internalFile.exists()) {
                        if (!internalFile.delete()) {
                            throw new Exception("El sistema de archivos denegó el borrado físico. El archivo ZIP podría estar en uso por otro programa (Archivos huérfanos prevenidos).");
                        }
                    }

                    // 2. Borrar de la DB (Si el físico falló, esta línea nunca se ejecuta)
                    libraryRepository.delete(book.id());

                    // 3. Refrescar ListView Frontend
                    System.out.println("🗑️ Recargando DB visual...");
                    loadBooksFromDatabase();
                } catch (Exception ex) {
                    mostrarError("Fallo crítico al intentar borrar: " + ex.getMessage());
                }
            }
        });
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operación fallida");
        alert.setContentText(mensaje);
        alert.show();
    }
}
