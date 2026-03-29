package com.oliver.presentation.controllers;

import com.oliver.core.application.ports.in.ConvertPdfToScormUseCase;
import com.oliver.core.application.ports.out.LibraryRepositoryPort;
import com.oliver.core.domain.models.ConversionResult;
import com.oliver.core.domain.models.Ebook;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Controlador de la "Sub-Página": Converter.
 * Acá va a vivir toda la lógica específica del Drag & Drop y Conversión.
 */
@Component
public class ConverterController {

    private final ConvertPdfToScormUseCase convertUseCase;
    private final LibraryRepositoryPort libraryRepository;

    private File selectedPdfFile;
    private static File lastKnownDirectory = null;

    @FXML
    private StackPane dropZone;
    @FXML
    private VBox dropBox;
    @FXML
    private Button btnSelectFile;
    @FXML
    private Button btnProcess;
    @FXML
    private Label lblFileInfo;
    @FXML
    private TextArea txtIndex;
    @FXML
    private TextField txtTitle;
    @FXML
    private ProgressBar convertProgressBar;
    @FXML
    private ProgressIndicator uploadProgress;
    @FXML
    private VBox progressContainer;
    @FXML
    private Label lblProgressText;
    @FXML
    private VBox recentBooksContainer;

    // Inyectamos el caso de uso por constructor (Spring Boot nos hace la magia)
    public ConverterController(ConvertPdfToScormUseCase convertUseCase, LibraryRepositoryPort libraryRepository) {
        this.convertUseCase = convertUseCase;
        this.libraryRepository = libraryRepository;
    }

    @FXML
    public void initialize() {
        System.out.println("✅ ConverterController inyectado.");
        setupDragAndDrop();
        loadRecentBooks();
    }

    private void setupDragAndDrop() {
        // Qué pasa cuando alguien arrastra algo POR ENCIMA de la zona (sin soltar)
        dropZone.setOnDragOver(event -> {
            if (!btnProcess.isDisabled() && event.getDragboard().hasFiles()) {
                File file = event.getDragboard().getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".pdf")) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
            }
            event.consume();
        });

        // Estilo dinámico: Cuando el archivo entra en el área
        dropZone.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                dropBox.setStyle(
                        "-fx-border-color: #000666; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 15px; -fx-padding: 40px; -fx-background-color: rgba(0, 6, 102, 0.05); -fx-background-radius: 15px;");
            }
            event.consume();
        });

        // Restaurar estilo: Cuando el archivo sale del área
        dropZone.setOnDragExited(event -> {
            dropBox.setStyle(
                    "-fx-border-color: rgba(118, 118, 131, 0.3); -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 15px; -fx-padding: 40px;");
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            boolean success = false;
            // Bloqueo de inyección si la app ya está procesando
            if (!btnProcess.isDisabled() && event.getDragboard().hasFiles()) {
                File file = event.getDragboard().getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".pdf")) {
                    registerSelectedFile(file);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    @FXML
    public void handleSelectFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Abrir Libro en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));

        // Si ya habíamos abierto uno antes, abrir el explorador ahí
        if (lastKnownDirectory != null && lastKnownDirectory.exists()) {
            fileChooser.setInitialDirectory(lastKnownDirectory);
        }

        File file = fileChooser.showOpenDialog(btnSelectFile.getScene().getWindow());
        if (file != null) {
            registerSelectedFile(file);
        }
    }

    private void registerSelectedFile(File file) {
        uploadProgress.setVisible(true);
        uploadProgress.setManaged(true);
        lblFileInfo.setText("Cargando archivo...");

        // Simular un tiny delay de carga visual
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(600));
        pause.setOnFinished(e -> {
            uploadProgress.setVisible(false);
            uploadProgress.setManaged(false);
            this.selectedPdfFile = file;
            lastKnownDirectory = file.getParentFile();
            lblFileInfo.setText("📄 " + file.getName() + " (" + (file.length() / 1024) + " KB)");
            lblFileInfo.setStyle("-fx-text-fill: #1A237E; -fx-font-weight: bold;");
        });
        pause.play();
    }

    @FXML
    public void handleProcessConvertion(ActionEvent event) {
        if (selectedPdfFile == null) {
            lblFileInfo.setText("⚠️ ¡Primero debes seleccionar un PDF!");
            lblFileInfo.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
            return;
        }

        // Recuperar el índice y título opcional
        String indexText = txtIndex.getText();
        String customTitle = txtTitle.getText();

        // UI State: Entramos en modo "Procesando" para bloquear clicks accidentales
        btnProcess.setDisable(true);
        btnProcess.setText("⏳ Procesando, no cierres...");
        btnSelectFile.setDisable(true);
        
        progressContainer.setVisible(true);
        progressContainer.setManaged(true);
        lblProgressText.setText("Procesando tu SCORM...");
        convertProgressBar.setProgress(0.0);

        // Hilo asíncrono estilo "Pro" (Task de JavaFX para no congelar la UI)
        Task<ConversionResult> conversionTask = new Task<ConversionResult>() {
            @Override
            protected ConversionResult call() throws Exception {
                // Esta línea bloquea el hilo secundario (Background thread)
                return convertUseCase.execute(selectedPdfFile, indexText, customTitle, progress -> {
                    Platform.runLater(() -> convertProgressBar.setProgress(progress));
                });
            }
        };

        // Qué hacer cuando termina PERFECTAMENTE o CON ERROR CAPTURADO internamente
        conversionTask.setOnSucceeded(e -> {
            ConversionResult result = conversionTask.getValue();

            // Volvemos al Hilo Principal (UI Thread) para modificar controles
            Platform.runLater(() -> {
                btnProcess.setDisable(false);
                btnSelectFile.setDisable(false);
                progressContainer.setVisible(false);
                progressContainer.setManaged(false);
                
                if (result.isSuccess()) {
                    btnProcess.setText("✨ Crear otra respuesta");
                    lblFileInfo.setText("✅ ¡eBook guardado!");
                    lblFileInfo.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    this.selectedPdfFile = null; // Reiniciamos estado
                    loadRecentBooks(); // <--- Recargar la lista al vuelo!
                } else {
                    btnProcess.setText("✨ Intentar de nuevo");
                    lblFileInfo.setText("⚠️ Ocurrió un error (ver detalles)");
                    lblFileInfo.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");

                    // Alerta emergente elegante
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error de Proceso");
                    alert.setHeaderText("No pudimos convertir tu PDF");
                    alert.setContentText(result.getMessage());
                    alert.showAndWait();
                }
            });
        });

        // Qué hacer si explota dramáticamente (Exception no detectada por el Core)
        conversionTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                btnProcess.setDisable(false);
                btnSelectFile.setDisable(false);
                progressContainer.setVisible(false);
                progressContainer.setManaged(false);
                btnProcess.setText("☠️ ¡Fallo Crítico!");
                lblFileInfo.setText("Excepción grave. Cierra y vuelve a abrir.");
                lblFileInfo.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");

                Throwable error = conversionTask.getException();
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error Crítico");
                alert.setHeaderText("Fallo en la aplicación");
                alert.setContentText(error != null ? error.getMessage() : "Error desconocido en hilo asíncrono.");
                alert.showAndWait();
            });
        });

        // Disparar el hilo paralelo
        new Thread(conversionTask).start();
    }

    private void loadRecentBooks() {
        if (recentBooksContainer == null)
            return;

        try {
            List<Ebook> books = libraryRepository.findAll();
            recentBooksContainer.getChildren().clear();

            if (books == null || books.isEmpty()) {
                showEmptyState();
                return;
            }

            // Mostrar últimos libros (máximo 5) en el converter
            int count = 0;
            // Reverse para mostrar el último generado arriba
            for (int i = books.size() - 1; i >= 0 && count < 5; i--) {
                recentBooksContainer.getChildren().add(createBookCard(books.get(i)));
                count++;
            }

        } catch (Exception e) {
            System.err.println("❌ Error al leer historial: " + e.getMessage());
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

        VBox texts = new VBox(2);
        texts.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("No se han creado eBooks todavía");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #0d1c2f;");
        Label desc = new Label("Sube un PDF para generar tu primer eBook.");
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #515f74;");

        texts.getChildren().addAll(title, desc);
        emptyState.getChildren().addAll(icon, texts);
        recentBooksContainer.getChildren().setAll(emptyState);
    }

    private HBox createBookCard(Ebook book) {
        HBox card = new HBox(15);
        card.getStyleClass().add("history-item");
        card.setStyle("-fx-padding: 15px;");
        card.setAlignment(Pos.CENTER_LEFT);

        Region icon = new Region();
        icon.setStyle(
                "-fx-background-color: #4CAF50; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 8px;");

        VBox texts = new VBox(2);
        texts.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(book.title());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0d1c2f; -fx-font-size: 14px;");

        Label dateLbl = new Label("Generado el: " + book.creationDate().replace("T", " "));
        dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #515f74;");

        texts.getChildren().addAll(titleLbl, dateLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnDownload = new Button("📥 Descargar");
        btnDownload.setStyle(
                "-fx-background-color: #000666; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        btnDownload.setOnAction(e -> descargarArchivo(book));

        card.getChildren().addAll(icon, texts, spacer, btnDownload);

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

        File selectedFile = fileChooser.showSaveDialog(recentBooksContainer.getScene().getWindow());

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
            
            copyTask.setOnSucceeded(event -> {
                loadingAlert.setResult(ButtonType.OK);
                loadingAlert.close();
                System.out.println("✅ SCORM descargado en: " + selectedFile.getAbsolutePath());
                
                Alert successAlert = new Alert(AlertType.INFORMATION);
                successAlert.setTitle("Éxito");
                successAlert.setHeaderText("¡Descarga Completada!");
                successAlert.setContentText("SCORM guardado correctamente.");
                successAlert.show();
            });
            
            copyTask.setOnFailed(event -> {
                loadingAlert.setResult(ButtonType.OK);
                loadingAlert.close();
                
                Alert errorAlert = new Alert(AlertType.ERROR);
                errorAlert.setHeaderText("Error de Descarga");
                errorAlert.setContentText(copyTask.getException().getMessage());
                errorAlert.show();
            });
            
            new Thread(copyTask).start();
        }
    }
}
