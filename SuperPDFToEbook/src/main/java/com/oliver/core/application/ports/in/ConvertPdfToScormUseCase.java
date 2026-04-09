package com.oliver.core.application.ports.in;

import com.oliver.core.domain.models.ConversionResult;
import java.io.File;

/**
 * Input Port (Puerto de Entrada):
 * El contrato oficial que la interfaz gráfica (UI) debe usar para hablar con el Core.
 * Define el "QUÉ" queremos, sin importar el "CÓMO".
 */
public interface ConvertPdfToScormUseCase {
    ConversionResult execute(File pdfFile, String textIndex, String customTitle, String organizationName, java.util.function.Consumer<Double> progressCallback);
}
