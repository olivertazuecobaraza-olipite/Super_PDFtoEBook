package com.oliver.presentation.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ScormPreviewHelper {

    public static void previewScormZip(String zipFilePath) {
        new Thread(() -> {
            try {
                File zipFile = new File(zipFilePath);
                if (!zipFile.exists()) {
                    showError("El archivo ZIP no existe.");
                    return;
                }

                // Usamos la carpeta del workspace para guardar previews temporales
                File workspaceDir = new File(System.getProperty("user.home"), ".superpdf_workspace");
                File previewsDir = new File(workspaceDir, ".previews");
                if (!previewsDir.exists()) previewsDir.mkdirs();
                
                // Intentamos reciclar la extracción si ya existe
                File extractDir = new File(previewsDir, zipFile.getName().replace(".zip", ""));
                if (!extractDir.exists()) {
                    extractDir.mkdirs();
                    unzip(zipFile, extractDir);
                }

                File indexHtml = new File(extractDir, "index.html");
                if (indexHtml.exists()) {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(indexHtml.toURI());
                    } else {
                        showError("Tu sistema operativo no soporta abrir navegadores desde Java de forma nativa.");
                    }
                } else {
                    showError("El paquete SCORM está corrupto: No tiene archivo index.html");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("No se pudo extraer o abrir el archivo SCORM: " + e.getMessage());
            }
        }).start();
    }

    private static void unzip(File zipFile, File destDir) throws Exception {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryDestination = new File(destDir, entry.getName());
                
                // Evitamos inyección de rutas (Zip Slip Vulnerability)
                if (!entryDestination.getCanonicalPath().startsWith(destDir.getCanonicalPath() + File.separator)) {
                    throw new Exception("Entrada ZIP inválida (fuga de directorio): " + entry.getName());
                }

                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    try (InputStream in = zip.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(entryDestination)) {
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                    }
                }
            }
        }
    }

    private static void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Error de Visualización");
            alert.setContentText(message);
            alert.show();
        });
    }
}
