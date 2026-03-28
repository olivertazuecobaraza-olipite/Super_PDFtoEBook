package com.oliver.core.application.ports.out;

import java.io.File;

import com.oliver.core.domain.models.EbookPagesMap;

/**
 * Output Port (Puerto de Salida):
 * El Core necesita extraer las páginas individuales de un PDF como imágenes, pero NO SABE cómo hacerlo.
 * Delega esta responsabilidad a quien implemente esta interfaz (ej: Apache PDFBox).
 */
public interface PdfExtractorPort {
    EbookPagesMap extractPages(File pdfFile, String userTextIndex) throws Exception;
}
