package com.oliver.core.domain.models;

/**
 * Entidad inmutable que representa un eBook en nuestra biblioteca.
 * Al usar un paradigma moderno de Java 17, usamos un Record.
 */
public record Ebook(String id, String title, String filePath, String creationDate) {}
