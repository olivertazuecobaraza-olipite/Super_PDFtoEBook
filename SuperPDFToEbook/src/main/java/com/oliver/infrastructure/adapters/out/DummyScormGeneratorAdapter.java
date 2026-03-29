package com.oliver.infrastructure.adapters.out;

import com.oliver.core.application.ports.out.ScormGeneratorPort;
import com.oliver.core.domain.models.EbookPagesMap;
import org.springframework.stereotype.Component;

/**
 * Adaptador Dummy antiguo para pruebas.
 * Ahora usamos el ZipScormGeneratorAdapter (Primary).
 */
@Component
public class DummyScormGeneratorAdapter implements ScormGeneratorPort {

    @Override
    public String generatePackage(String title, EbookPagesMap pagesMap, java.util.function.Consumer<Double> progressCallback) throws Exception {
        System.out.println("⚠️ [DUMMY Generator] Simulando la creación SCORM DUMMY para: " + title);
        System.out.println("⚠️ Directorio recibido con páginas: " + pagesMap.tempDirectory().getAbsolutePath());
        
        if (progressCallback != null) progressCallback.accept(1.0);
        return "C:\\Ruta\\Ficticia\\Paquete_Dummy_" + title.replace(" ", "_") + ".zip";
    }
}
