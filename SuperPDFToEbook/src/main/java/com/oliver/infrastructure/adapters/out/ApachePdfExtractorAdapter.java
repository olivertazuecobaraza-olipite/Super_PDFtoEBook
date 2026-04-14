package com.oliver.infrastructure.adapters.out;

import com.oliver.core.application.ports.out.PdfExtractorPort;
import com.oliver.core.domain.models.EbookPagesMap;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Adaptador de Salida (Infrastructure) para la extracción de PDFs.
 * Implementa la lógica de renderizado y extracción de texto por página.
 */
@Component
public class ApachePdfExtractorAdapter implements PdfExtractorPort {

    @Override
    public EbookPagesMap extractPages(File pdfFile, String userTextIndex, java.util.function.Consumer<Double> progressCallback) throws Exception {
        System.out.println("📄 [PDFBox] Renderizando páginas y extrayendo Texto/TTS a disco: " + pdfFile.getName());
        
        String workspacePath = System.getProperty("user.home") + File.separator + ".superpdf_workspace";
        File tempDir = new File(workspacePath, "temp_render_" + UUID.randomUUID().toString());
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new Exception("No se pudo crear el directorio temporal para renderizado.");
        }

        try {
            // Usamos MemoryUsageSetting.setupTempFileOnly() para que los PDFs gigantes (1000+ páginas) NO colapsen la Memoria RAM.
            try (PDDocument document = PDDocument.load(pdfFile, org.apache.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())) {
                PDFTextStripper stripper = new PDFTextStripper();
            
                int totalPages = document.getNumberOfPages();
                
                // Resolución de Árbol de Jerarquía de Índice (Prioridades)
                String outlineHtml;
                if (userTextIndex != null && !userTextIndex.trim().isEmpty()) {
                    System.out.println("📝 Utilizando índice provisto manualmente desde la Interfaz de Usuario...");
                    outlineHtml = parseUserTextIndexToHtml(userTextIndex);
                } else {
                    PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
                    if (outline != null && outline.getFirstChild() != null) {
                        System.out.println("📑 Índice (Outline) nativo capturado con éxito.");
                        outlineHtml = extractOutlineHTML(outline, document);
                    } else {
                        System.out.println("⚠️ El PDF no tiene marcadores. Generando índice de paginación de respaldo.");
                        outlineHtml = buildFallbackOutline(totalPages);
                    }
                }

                // NUEVO: Copiamos el archivo PDF original tal cual al espacio temporal
                File targetPdfFile = new File(tempDir, "document.pdf");
                Files.copy(pdfFile.toPath(), targetPdfFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                System.out.println("⏳ Procesando extracción de texto para " + totalPages + " páginas...");
                for (int i = 0; i < totalPages; i++) {
                    // Notificar progreso (0.0 a 1.0)
                    if (progressCallback != null) {
                        progressCallback.accept((double) (i + 1) / totalPages);
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("El usuario o el sistema cerró la aplicación durante el procesado.");
                    }
                    int pageNum = i + 1;

                    stripper.setStartPage(pageNum);
                    stripper.setEndPage(pageNum);
                    String pageText = stripper.getText(document);
                    File textFile = new File(tempDir, pageNum + ".txt");
                    Files.writeString(textFile.toPath(), pageText);
                }

                System.out.println("✅ [PDFBox] Todas las páginas volcadas. Documento PDF y Textos TTS disponibles.");
                return new EbookPagesMap(tempDir, totalPages, outlineHtml);
            }
        } catch (Exception e) {
            // CRITICO: Limpieza de basuras huérfanas si falla a la mitad del proceso
            deleteDirectory(tempDir);
            throw new Exception("Fallo en la extracción del PDF, archivos temporales revertidos: " + e.getMessage(), e);
        }
    }
    
    // Parser manual para el textarea del usuario (La Jerarquía #1)
    private String parseUserTextIndexToHtml(String userTextIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"padding-bottom: 10px; opacity:0.7; font-size: 0.9em; font-style: italic;\">")
          .append("Índice de autor:")
          .append("</div>");
        sb.append("<ul class=\"outline-list\">");
        
        String[] lines = userTextIndex.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            int lastDashIndex = line.lastIndexOf('-');
            if (lastDashIndex != -1 && lastDashIndex < line.length() - 1) {
                String title = line.substring(0, lastDashIndex).trim();
                String pageStr = line.substring(lastDashIndex + 1).trim();
                
                try {
                    int pageNum = Integer.parseInt(pageStr);
                    sb.append("<li><a href=\"#\" onclick=\"window.goToPage(").append(pageNum).append("); return false;\">")
                      .append(escapeHtml(title)).append("</a></li>");
                } catch (NumberFormatException e) {
                    sb.append("<li><span style=\"opacity:0.8\">").append(escapeHtml(line)).append("</span></li>");
                }
            } else {
                sb.append("<li><span style=\"opacity:0.8\">").append(escapeHtml(line)).append("</span></li>");
            }
        }
        sb.append("</ul>");
        return sb.toString();
    }
    
    // Generador Failsafe para PDFs sin índice (La Jerarquía #3)
    private String buildFallbackOutline(int totalPages) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"padding-bottom: 10px; opacity:0.7; font-size: 0.9em; font-style: italic;\">")
          .append("El PDF original no contiene un índice de capítulos. Mostrando saltos rápidos:")
          .append("</div>");
        sb.append("<ul class=\"outline-list\">");
        sb.append("<li><a href=\"#\" onclick=\"window.goToPage(1); return false;\">▶ Inicio (Página 1)</a></li>");
        
        int step = totalPages > 50 ? 10 : 5;
        for (int i = step; i < totalPages; i += step) {
            sb.append("<li><a href=\"#\" onclick=\"window.goToPage(").append(i).append("); return false;\">Página ").append(i).append("</a></li>");
        }
        
        if (totalPages > 1) {
            sb.append("<li><a href=\"#\" onclick=\"window.goToPage(").append(totalPages).append("); return false;\">◼ Final (Página ").append(totalPages).append(")</a></li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    // Navegación recursiva del DOM de los marcadores del PDF original (La Jerarquía #2)
    private String extractOutlineHTML(PDOutlineNode outline, PDDocument doc) {
        if (outline == null || outline.getFirstChild() == null) return "<ul></ul>";
        StringBuilder sb = new StringBuilder();
        sb.append("<ul class=\"outline-list\">");
        
        PDOutlineItem current = outline.getFirstChild();
        while (current != null) {
            sb.append("<li>");
            String title = current.getTitle();
            int pageNum = -1;
            
            try {
                PDPage page = current.findDestinationPage(doc);
                if (page != null) {
                    pageNum = doc.getPages().indexOf(page) + 1;
                }
            } catch (Exception e) {}
            
            if (pageNum != -1) {
                sb.append("<a href=\"#\" onclick=\"window.goToPage(").append(pageNum).append("); return false;\">")
                  .append(escapeHtml(title)).append("</a>");
            } else {
                sb.append("<span>").append(escapeHtml(title)).append("</span>");
            }
            
            if (current.getFirstChild() != null) {
                sb.append(extractOutlineHTML(current, doc));
            }
            
            sb.append("</li>");
            current = current.getNextSibling();
        }
        sb.append("</ul>");
        return sb.toString();
    }
    
    private void deleteDirectory(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
