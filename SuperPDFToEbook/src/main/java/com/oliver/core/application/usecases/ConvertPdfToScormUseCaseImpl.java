package com.oliver.core.application.usecases;

import com.oliver.core.application.ports.in.ConvertPdfToScormUseCase;
import com.oliver.core.application.ports.out.PdfExtractorPort;
import com.oliver.core.application.ports.out.ScormGeneratorPort;
import com.oliver.core.application.ports.out.LibraryRepositoryPort;
import com.oliver.core.domain.models.ConversionResult;
import com.oliver.core.domain.models.EbookPagesMap;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * La joya de la corona. El caso de uso puro.
 * Orquesta la lógica del negocio delegando detalles técnicos a los puertos.
 * Actualizado con el diseño de Visor de Imágenes: Implementa limpieza de basuras temporales.
 */
@Service
public class ConvertPdfToScormUseCaseImpl implements ConvertPdfToScormUseCase {

    private final PdfExtractorPort pdfExtractor;
    private final ScormGeneratorPort scormGenerator;
    private final LibraryRepositoryPort libraryRepository;

    public ConvertPdfToScormUseCaseImpl(PdfExtractorPort pdfExtractor, ScormGeneratorPort scormGenerator, LibraryRepositoryPort libraryRepository) {
        this.pdfExtractor = pdfExtractor;
        this.scormGenerator = scormGenerator;
        this.libraryRepository = libraryRepository;
    }

    @Override
    public ConversionResult execute(File pdfFile, String textIndex) {
        if (pdfFile == null || !pdfFile.exists()) {
            return new ConversionResult(false, "El archivo PDF proporcionado no existe o fue movido.", null);
        }

        EbookPagesMap pagesMap = null;
        try {
            System.out.println("🔄 Solicitando extracción y renderizado a disco desde PDFBox...");
            // Extraer a imágenes JPG temporales en disco y mandar índice manual
            pagesMap = pdfExtractor.extractPages(pdfFile, textIndex);
            
            System.out.println("📦 Construyendo empaquetado SCORM con Activos Estaticos e Imágenes...");
            String packageTitle = pdfFile.getName().replace(".pdf", " - eBook");
            String outputPath = scormGenerator.generatePackage(packageTitle, pagesMap);
            
            // Grabación de metadatos en biblioteca local SQLite/CSV
            String uuid = UUID.randomUUID().toString();
            libraryRepository.save(uuid, packageTitle, outputPath, LocalDateTime.now());
            
            System.out.println("✅ ¡eBook Visor generado con éxito!");
            return new ConversionResult(true, "¡eBook interactivo generado con éxito! Paquete SCORM preparado en:\n" + outputPath, outputPath);

        } catch (Throwable e) {
            e.printStackTrace();
            String errorMsg = (pagesMap == null) ? "Fallo estrepitoso leyendo el PDF" : "Fallo generando el Empaquetado SCORM o guardando en Biblioteca";
            return new ConversionResult(false, errorMsg + ": " + e.getMessage(), null);
        } finally {
            // CRITICO: Bloque finally para barrer la casa. Java se va y no deja rastros (No Fugas de disco duro)
            if (pagesMap != null && pagesMap.tempDirectory() != null) {
                System.out.println("🧹 Limpiando los JPG temporales...");
                deleteDirectory(pagesMap.tempDirectory());
            }
        }
    }
    
    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    }
                    file.delete();
                }
            }
            dir.delete();
        }
    }
}
