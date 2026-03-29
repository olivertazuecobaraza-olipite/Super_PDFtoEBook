package com.oliver.core.application.usecases;

import com.oliver.core.application.ports.out.LibraryRepositoryPort;
import com.oliver.core.application.ports.out.PdfExtractorPort;
import com.oliver.core.application.ports.out.ScormGeneratorPort;
import com.oliver.core.domain.models.ConversionResult;
import com.oliver.core.domain.models.EbookPagesMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConvertPdfToScormUseCaseImplTest {

    @Mock
    private PdfExtractorPort pdfExtractor;

    @Mock
    private ScormGeneratorPort scormGenerator;

    @Mock
    private LibraryRepositoryPort libraryRepository;

    @InjectMocks
    private ConvertPdfToScormUseCaseImpl useCase;

    private File mockPdfFile;

    @BeforeEach
    void setUp() throws Exception {
        // Creamos un archivo temporal físico para simular un PDF real
        mockPdfFile = File.createTempFile("test_document", ".pdf");
        mockPdfFile.deleteOnExit(); // Se limpia automáticamente al cerrar la JVM
    }

    @Test
    @DisplayName("Debe fallar rápidamente si el archivo PDF pasado es nulo o no existe")
    void testExecute_PdfNullOrNotExists() {
        // Ejecución (Act)
        ConversionResult result = useCase.execute(null, "", null, null);

        // Verificamos el Modelo de Dominio devuelto (Assert)
        assertFalse(result.isSuccess(), "Si pasamos un PDF nulo, debe rebotarlo inmediatamente.");
        assertTrue(result.getMessage().contains("no existe o fue movido"), "El mensaje de error debe explicar el problema de I/O.");
        
        // Verificamos que los puertos JAMÁS hayan sido llamados (Protección de Hexágono)
        verifyNoInteractions(pdfExtractor, scormGenerator, libraryRepository);
    }

    @Test
    @DisplayName("Debe orquestar el Flujo Perfecto (Happy Path) exitosamente")
    void testExecute_HappyPathSuccess() throws Exception {
        // Configurar los mocks (Arrange)
        // Simulamos que el adaptador extractor de Apache nos devuelve una carpeta ficticia con 5 páginas
        File fakeTempDir = new File(System.getProperty("java.io.tmpdir"), "fake_render");
        fakeTempDir.mkdirs();
        EbookPagesMap fakeMap = new EbookPagesMap(fakeTempDir, 5, "<ul><li>Index</li></ul>");
        
        when(pdfExtractor.extractPages(eq(mockPdfFile), eq("Índice customizado"), any())).thenReturn(fakeMap);
        when(scormGenerator.generatePackage(anyString(), eq(fakeMap), any())).thenReturn("/path/to/SCORM_Output.zip");

        // Ejecución (Act)
        ConversionResult result = useCase.execute(mockPdfFile, "Índice customizado", "Titulo Test", null);

        // Afirmaciones (Assert)
        assertTrue(result.isSuccess(), "La conversión debe retornar True en el caso de éxito.");
        assertEquals("/path/to/SCORM_Output.zip", result.getOutputPath(), "La ruta del archivo final SCORM debe coincidir con la arrojada por el Generador.");

        // Verificamos que el repositorio de guardado haya sigo invocado 1 sola vez
        verify(libraryRepository, times(1)).save(anyString(), anyString(), eq("/path/to/SCORM_Output.zip"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Debe interceptar caídas críticas del extractor de PDFs (Ej: Archivo corrupto/Memory Crash)")
    void testExecute_ExtractorThrowsException() throws Exception {
        // Configurar los mocks (Arrange)
        // Forzamos a que PDFBox simule explotar por falta de RAM o un PDF corrupto
        when(pdfExtractor.extractPages(any(File.class), anyString(), any()))
                .thenThrow(new RuntimeException("Inyección emulada de Falla en Apache PDFBox"));

        // Ejecución (Act)
        ConversionResult result = useCase.execute(mockPdfFile, null, null, null);

        // Afirmaciones (Assert)
        assertFalse(result.isSuccess(), "El caso de uso debe capturar el crash y devolver un ConversionResult con error controlado (Graceful Fail).");
        assertTrue(result.getMessage().contains("Fallo estrepitoso leyendo el PDF"), "Debe inyectar nuestro mensaje de fallback.");
        
        // Comprobamos que como falló la extracción, NUNCA se generó el ZIP ni se grabó basura en la DB.
        verifyNoInteractions(scormGenerator, libraryRepository);
    }
}
