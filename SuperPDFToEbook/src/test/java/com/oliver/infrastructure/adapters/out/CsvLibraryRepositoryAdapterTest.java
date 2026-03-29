package com.oliver.infrastructure.adapters.out;

import com.oliver.core.domain.models.Ebook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CsvLibraryRepositoryAdapterTest {

    private CsvLibraryRepositoryAdapter repository;
    private final String MOCK_DB_NAME = ".test_superpdf_library_db.csv";
    private File testDbFile;

    @BeforeEach
    void setUp() {
        // Inicializamos el repositorio inyectando manualmente un nombre temporal de test para el CSV
        testDbFile = new File(System.getProperty("user.home"), MOCK_DB_NAME);
        // Usamos nuestro Factory/Constructor Especial de Testing (Hexagonal DI)
        repository = new CsvLibraryRepositoryAdapter(testDbFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() throws Exception {
        // Limpiamos el basurero post-testing para no dejar mugre en el disco C:\ o Macintosh HD
        if (testDbFile.exists()) {
            Files.delete(testDbFile.toPath());
        }
    }

    @Test
    @DisplayName("Debe persistir correctamente un libro y luego cargarlo evadiendo CRLF (Log Injection)")
    void testSaveAndLoad_EvitesCsvInjection() throws Exception {
        String testId = UUID.randomUUID().toString();
        // Inyectamos saltos de línea maliciosos para ver si el Escudado funciona
        String maliciousTitle = "Mi Novela\n\r Secreta, Autor"; 
        String fakePath = "/test/path/novela_secreta.zip";
        
        // Ejecución (Act)
        repository.save(testId, maliciousTitle, fakePath, LocalDateTime.now());
        List<Ebook> library = repository.findAll();
        
        // Afirmaciones (Assert)
        assertFalse(library.isEmpty(), "La librería no debe estar vacía tras persistir un récord.");
        assertEquals(1, library.size(), "Debe haber exactamente un solo libro en el CSV.");
        
        Ebook found = library.get(0);
        assertEquals(testId, found.id(), "El UUID debe persistir intácto.");
        // Verificamos que el título fue curado reemplazando \n \r por vacío/espacio y las comas por guiones
        assertFalse(found.title().contains("\n"), "Ningún título debe tener salto de línea interno tras persistir.");
        assertEquals("Mi Novela   Secreta - Autor", found.title(), "La Inyección debió sanitizarse íntegramente");
    }

    @Test
    @DisplayName("Debe eliminar Físicamente un renglón del CSV sin bloquear ni romper archivos")
    void testDelete_RemovesExactRowFromCsv() throws Exception {
        // Arrange
        String doc1Id = UUID.randomUUID().toString();
        String doc2Id = UUID.randomUUID().toString();
        repository.save(doc1Id, "Libro A", "/path/a", LocalDateTime.now());
        repository.save(doc2Id, "Libro B", "/path/b", LocalDateTime.now());
        
        // Ejecución (Act)
        repository.delete(doc1Id);
        List<Ebook> library = repository.findAll();
        
        // Afirmaciones (Assert)
        assertEquals(1, library.size(), "Se borró el Libro A, sólo debe quedar 1.");
        assertEquals("Libro B", library.get(0).title(), "El libro que sobrevive DEBE ser el Libro B.");
        assertEquals(doc2Id, library.get(0).id(), "El UUID no debe mutar tras la regrabación completa del archivo");
    }
}
