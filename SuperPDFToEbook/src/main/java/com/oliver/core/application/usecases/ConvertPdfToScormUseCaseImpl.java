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
    public ConversionResult execute(File pdfFile, String textIndex, String customTitle, java.util.function.Consumer<Double> progressCallback) {
        if (pdfFile == null || !pdfFile.exists()) {
            return new ConversionResult(false, "El archivo PDF no existe o fue movido.", null);
        }
        
        System.out.println("🚀 [Dominio] Iniciando conversión para: " + pdfFile.getName());

        EbookPagesMap pagesMap = null;
        try {
            // Fase 1: Extracción del PDF (Representa el 50% del total)
            pagesMap = pdfExtractor.extractPages(pdfFile, textIndex, progress -> {
                if (progressCallback != null) {
                    progressCallback.accept(progress * 0.5); 
                }
            });
            System.out.println("✅ [Dominio] Extracción completada.");
            
            // Determinar Título
            String title = (customTitle == null || customTitle.trim().isEmpty()) 
                    ? pdfFile.getName().replace(" ", "_").replace(".pdf", "")
                    : customTitle.trim();

            // Fase 2: Empaquetado a SCORM (Representa el otro 50%)
            String outputPath = scormGenerator.generatePackage(title, pagesMap, progress -> {
                if (progressCallback != null) {
                    progressCallback.accept(0.5 + (progress * 0.5)); 
                }
            });
            System.out.println("✅ [Dominio] SCORM generado en: " + outputPath);
            
            // Fase 3: Registro en la base de datos (0%)
            String id = UUID.randomUUID().toString();
            libraryRepository.save(id, title, outputPath, LocalDateTime.now());
            
            return new ConversionResult(true, "¡Conversión SCORM 100% exitosa!", outputPath);

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
