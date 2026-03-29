package com.oliver.infrastructure.adapters.out;

import com.oliver.core.domain.models.EbookPagesMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class ZipScormGeneratorAdapterTest {

    private ZipScormGeneratorAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        adapter = new ZipScormGeneratorAdapter();
    }

    @Test
    @DisplayName("Debe generar un archivo ZIP con la estructura SCORM correcta")
    void testGeneratePackage_CorrectStructure() throws Exception {
        // Arrange
        String title = "Test eBook";
        File pagesDir = tempDir.resolve("pages").toFile();
        pagesDir.mkdirs();
        
        // Crear una página dummy (imagen y texto)
        Files.writeString(tempDir.resolve("pages/1.txt"), "Contenido de la página 1");
        File imgFile = tempDir.resolve("pages/1.jpg").toFile();
        try (FileOutputStream fos = new FileOutputStream(imgFile)) {
            fos.write(new byte[]{0, 1, 2, 3}); // Fake JPG content
        }

        EbookPagesMap pagesMap = new EbookPagesMap(pagesDir, 1, "<ul><li>Page 1</li></ul>");

        // Act
        String zipPath = adapter.generatePackage(title, pagesMap, progress -> {});

        // Assert
        assertNotNull(zipPath);
        File zipFile = new File(zipPath);
        assertTrue(zipFile.exists());
        assertTrue(zipFile.getName().endsWith(".zip"));

        // Verificar contenido del ZIP
        try (ZipFile zip = new ZipFile(zipFile)) {
            assertNotNull(zip.getEntry("imsmanifest.xml"), "Debe contener imsmanifest.xml");
            assertNotNull(zip.getEntry("index.html"), "Debe contener index.html");
            assertNotNull(zip.getEntry("css/styles.css"), "Debe contener css/styles.css");
            assertNotNull(zip.getEntry("js/viewer.js"), "Debe contener js/viewer.js");
            assertNotNull(zip.getEntry("assets/js/texts.js"), "Debe contener assets/js/texts.js");
            assertNotNull(zip.getEntry("assets/pages/1.jpg"), "Debe contener assets/pages/1.jpg");
            
            // Verificar inyección de texto en texts.js
            ZipEntry textsEntry = zip.getEntry("assets/js/texts.js");
            String textsJs = new String(zip.getInputStream(textsEntry).readAllBytes());
            assertTrue(textsJs.contains("Contenido de la página 1"), "El JS de textos debe contener el texto de la página");
        } finally {
            // Cleanup
            zipFile.delete();
        }
    }

    @Test
    @DisplayName("Debe sanitizar el título para el nombre del archivo ZIP")
    void testGeneratePackage_SanitizesTitle() throws Exception {
        // Arrange
        String maliciousTitle = "Title/With\\Bad:Chars?*\"<>|";
        EbookPagesMap pagesMap = new EbookPagesMap(tempDir.toFile(), 0, "");

        // Act
        String zipPath = adapter.generatePackage(maliciousTitle, pagesMap, null);

        // Assert
        File zipFile = new File(zipPath);
        assertFalse(zipFile.getName().contains("/"), "El nombre del archivo no debe contener barras");
        assertFalse(zipFile.getName().contains("\\"), "El nombre del archivo no debe contener barras invertidas");
        zipFile.delete();
    }
}
