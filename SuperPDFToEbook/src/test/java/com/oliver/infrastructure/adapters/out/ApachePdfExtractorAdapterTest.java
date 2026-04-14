package com.oliver.infrastructure.adapters.out;

import com.oliver.core.domain.models.EbookPagesMap;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ApachePdfExtractorAdapterTest {

    private ApachePdfExtractorAdapter adapter;
    private File realPdfFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        adapter = new ApachePdfExtractorAdapter();
        
        // Generar un PDF real de 2 páginas con PDFBox para el test
        realPdfFile = tempDir.resolve("test_fixture.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            // Página 1
            PDPage page1 = new PDPage();
            doc.addPage(page1);
            try (PDPageContentStream contents = new PDPageContentStream(doc, page1)) {
                contents.beginText();
                contents.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contents.newLineAtOffset(100, 700);
                contents.showText("Hello Page 1 Content");
                contents.endText();
            }

            // Página 2
            PDPage page2 = new PDPage();
            doc.addPage(page2);
            try (PDPageContentStream contents = new PDPageContentStream(doc, page2)) {
                contents.beginText();
                contents.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contents.newLineAtOffset(100, 700);
                contents.showText("Hello Page 2 Content");
                contents.endText();
            }

            // Añadir Outline (Marcadores)
            PDDocumentOutline outline = new PDDocumentOutline();
            doc.getDocumentCatalog().setDocumentOutline(outline);
            PDOutlineItem item = new PDOutlineItem();
            item.setTitle("Capitulo 1");
            item.setDestination(page1);
            outline.addLast(item);

            doc.save(realPdfFile);
        }
    }

    @Test
    @DisplayName("Debe extraer páginas (imágenes y texto) y generar el índice HTML")
    void testExtractPages_RealPdf() throws Exception {
        // Act
        AtomicReference<Double> lastProgress = new AtomicReference<>(0.0);
        EbookPagesMap result = adapter.extractPages(realPdfFile, null, lastProgress::set);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.totalPages(), "El PDF fixture tiene 2 páginas.");
        assertTrue(result.outlineHtml().contains("Capitulo 1"), "El índice HTML debe contener el marcador del PDF.");
        assertTrue(result.outlineHtml().contains("window.goToPage(1)"), "Debe tener el enlace JS a la página 1.");
        assertEquals(1.0, lastProgress.get(), "El progreso debe haber llegado al 100% (1.0).");

        // Verificar archivos físicos
        File workspace = result.tempDirectory();
        assertTrue(workspace.exists());
        assertTrue(new File(workspace, "document.pdf").exists(), "Debe existir el clon del PDF original.");
        assertTrue(new File(workspace, "1.txt").exists(), "Debe existir el texto de la página 1.");
        assertTrue(new File(workspace, "2.txt").exists(), "Debe existir el texto de la página 2.");
        
        // Verificar contenido del texto extraído
        String page1Text = java.nio.file.Files.readString(new File(workspace, "1.txt").toPath());
        assertTrue(page1Text.contains("Hello Page 1 Content"), "El texto extraído debe ser correcto.");

        // Cleanup del workspace (El adaptador no lo borra si tiene éxito, es trabajo del UseCase o el recolector de basura)
        deleteDirectory(workspace);
    }

    @Test
    @DisplayName("Debe usar el índice manual si el usuario lo provee")
    void testExtractPages_ManualIndex() throws Exception {
        // Arrange
        String manualIndex = "Prefacio - 1\nIntroduccion - 2";

        // Act
        EbookPagesMap result = adapter.extractPages(realPdfFile, manualIndex, null);

        // Assert
        assertTrue(result.outlineHtml().contains("Prefacio"), "El HTML debe contener el índice manual.");
        assertTrue(result.outlineHtml().contains("Introduccion"), "El HTML debe contener el índice manual.");
        assertTrue(result.outlineHtml().contains("Índice de autor"), "Debe indicar que es un índice de autor.");
        
        deleteDirectory(result.tempDirectory());
    }

    @Test
    @DisplayName("Debe generar un índice de respaldo si el PDF no tiene marcadores")
    void testExtractPages_FallbackIndex() throws Exception {
        // Arrange: Crear un PDF sin outline
        File noOutlinePdf = tempDir.resolve("no_outline.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.save(noOutlinePdf);
        }

        // Act
        EbookPagesMap result = adapter.extractPages(noOutlinePdf, null, null);

        // Assert
        assertTrue(result.outlineHtml().contains("El PDF original no contiene un índice de capítulos"), "Debe mostrar el mensaje de fallback.");
        assertTrue(result.outlineHtml().contains("Página 1"), "Debe tener saltos rápidos de página.");

        deleteDirectory(result.tempDirectory());
    }

    private void deleteDirectory(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) deleteDirectory(file);
                    else file.delete();
                }
            }
            dir.delete();
        }
    }
}
