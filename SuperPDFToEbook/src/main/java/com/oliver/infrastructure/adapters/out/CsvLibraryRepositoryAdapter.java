package com.oliver.infrastructure.adapters.out;

import com.oliver.core.application.ports.out.LibraryRepositoryPort;
import com.oliver.core.domain.models.Ebook;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adaptador de Infraestructura para persistir en una base de datos basada en archivos locales.
 * En el futuro se puede reemplazar por H2 o MongoDB alterando solo este archivo.
 */
@Component
public class CsvLibraryRepositoryAdapter implements LibraryRepositoryPort {

    // Archivo maestro guardado discretamente en la carpeta de usuario
    private String dbPath = System.getProperty("user.home") + File.separator + ".superpdf_library_db.csv";

    // Constructor default para la inyección mágica de Spring
    public CsvLibraryRepositoryAdapter() {}
    
    // Constructor inyectable para Software Testing
    public CsvLibraryRepositoryAdapter(String customPath) {
        this.dbPath = customPath;
    }

    @Override
    public synchronized void save(String id, String title, String path, LocalDateTime date) throws Exception {
        System.out.println("💾 [Database] Guardando nuevo eBook en la biblioteca: " + title);
        
        File dbFile = new File(dbPath);
        boolean fileExists = dbFile.exists();
        
        // Uso de try-with-resources asegura el cierre del recurso
        try (FileWriter fw = new FileWriter(dbFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            // Si el archivo no existía, añadimos la cabecera CSV
            if (!fileExists) {
                pw.println("ID,TITLE,FILE_PATH,CREATED_AT");
            }
            
            // Escapamos comas y SALTOS DE LÍNEA para que no rompa el CSV simple
            String safeTitle = title != null ? title.replace(",", " -").replace("\r", " ").replace("\n", " ") : "Untitled";
            String safePath = path != null ? path.replace(",", " ").replace("\r", "").replace("\n", "") : "";
            String dateFormatted = date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            pw.println(id + "," + safeTitle + "," + safePath + "," + dateFormatted);
        }
        
        System.out.println("✅ [Database] eBook persistido correctamente en: " + dbPath);
    }

    @Override
    public synchronized List<Ebook> findAll() throws Exception {
        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            return Collections.emptyList();
        }
        
        List<Ebook> books = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dbFile))) {
            String line = br.readLine(); // saltar cabecera
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    books.add(new Ebook(parts[0], parts[1], parts[2], parts[3]));
                }
            }
        }
        return books;
    }

    @Override
    public synchronized void delete(String id) throws Exception {
        File dbFile = new File(dbPath);
        if (!dbFile.exists()) return;

        List<String> remainingLines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dbFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Conservamos la línea si es la cabecera O si el ID no coincide
                if (line.startsWith("ID,") || !line.startsWith(id + ",")) {
                    remainingLines.add(line);
                }
            }
        }

        // Rescribimos el archivo con los que sobrevivieron
        try (FileWriter fw = new FileWriter(dbFile, false);
             PrintWriter pw = new PrintWriter(fw)) {
            for (String line : remainingLines) {
                pw.println(line);
            }
        }
        System.out.println("🗑️ [Database] Registro " + id + " eliminado correctamente.");
    }
}
