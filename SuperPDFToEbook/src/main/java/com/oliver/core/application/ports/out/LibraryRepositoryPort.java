package com.oliver.core.application.ports.out;

import com.oliver.core.domain.models.Ebook;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Puerto de Salida para la Persistencia (Base de Datos).
 * Define que el Core necesita guardar un libro cuando haya sido exportado correctamente,
 * desligando al Core de saber si esto se guarda en MySQL, MongoDB o un CSV.
 */
public interface LibraryRepositoryPort {
    void save(String id, String title, String path, LocalDateTime date) throws Exception;
    
    // El frontend necesita recuperar la lista de libros
    List<Ebook> findAll() throws Exception;
    
    // Método para borrar de la base de datos
    void delete(String id) throws Exception;
}
