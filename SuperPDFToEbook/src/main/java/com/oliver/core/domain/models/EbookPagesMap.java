package com.oliver.core.domain.models;

import java.io.File;

/**
 * Modelo de negocio: Representa el resultado de la extracción física de las páginas.
 * Almacena el directorio temporal de trabajo, el conteo total de páginas,
 * y el índice interactivo en formato HTML para inyección directa.
 */
public record EbookPagesMap(File tempDirectory, int totalPages, String outlineHtml) {
}
