package com.oliver.core.domain.models;

/**
 * Entidad de Dominio: Representa el resultado de nuestro intento de conversión.
 * Es puro Java, no depende de ningún framework ni librería externa.
 */
public class ConversionResult {
    private final boolean success;
    private final String message;
    private final String outputPath;

    public ConversionResult(boolean success, String message, String outputPath) {
        this.success = success;
        this.message = message;
        this.outputPath = outputPath;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getOutputPath() {
        return outputPath;
    }
}
