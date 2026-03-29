package com.oliver.core.application.ports.out;

import com.oliver.core.domain.models.EbookPagesMap;

/**
 * Output Port (Puerto de Salida):
 * El Core necesita empaquetar todo a formato SCORM.
 * Quien implemente esta interfaz se peleará con los archivos ZIP, los XML de Moodle 
 * y la inyección de las plantillas estáticas con las imágenes.
 */
public interface ScormGeneratorPort {
    String generatePackage(String title, EbookPagesMap pagesMap, java.util.function.Consumer<Double> progressCallback) throws Exception;
}
