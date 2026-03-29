package com.oliver.infrastructure.adapters.out;

import com.oliver.core.application.ports.out.ScormGeneratorPort;
import com.oliver.core.domain.models.EbookPagesMap;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Adaptador de Salida para construir el ZIP real del SCORM.
 * Ahora inyecta la variable OUTLINE y soporta la migración dinámica de los .txt para audiolibro.
 */
@Component
@Primary
public class ZipScormGeneratorAdapter implements ScormGeneratorPort {

    @Override
    public String generatePackage(String title, EbookPagesMap pagesMap, java.util.function.Consumer<Double> progressCallback) throws Exception {
        System.out.println("📦 [Zip SCORM] Empaquetando SCORM Interactivo (Index + Audio)...");
        
        String workspacePath = System.getProperty("user.home") + File.separator + ".superpdf_workspace";
        File dir = new File(workspacePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("No se pudo crear el directorio de trabajo: " + workspacePath + " (Posible falta de permisos)");
        }

        String safeTitle = title.replaceAll("[^a-zA-Z0-9.-]", "_");
        File zipFile = new File(dir, safeTitle + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + ".zip");
        
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            addFileToZip(zos, "imsmanifest.xml", buildManifest(title).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            String rawHtml = readTemplate("templates/scorm/index.html");
            // Doble inyección: el total de páginas y el HTML estático del menú Outline
            String mappedHtml = rawHtml.replace("/*[[TOTAL_PAGES]]*/ 1", String.valueOf(pagesMap.totalPages()))
                                       .replace("<!-- [[OUTLINE_HTML]] -->", pagesMap.outlineHtml());
                                       
            addFileToZip(zos, "index.html", mappedHtml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            String cssCode = readTemplate("templates/scorm/css/styles.css");
            addFileToZip(zos, "css/styles.css", cssCode.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            String jsCode = readTemplate("templates/scorm/js/viewer.js");
            addFileToZip(zos, "js/viewer.js", jsCode.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // Empaquetar activos Multimedia y armar JS dinámico Offline (Bypass a Web CORS)
            File tempDir = pagesMap.tempDirectory();
            StringBuilder jsTextArray = new StringBuilder("window.SCORM_PAGES_TEXT = [\"\""); // Index 0 = Null padding
            
            System.out.println("💾 Empaquetando activos Multimedia en ZIP...");
            for (int i = 1; i <= pagesMap.totalPages(); i++) {
                File txtFile = new File(tempDir, i + ".txt");
                if (txtFile.exists()) {
                    String content = Files.readString(txtFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                    // Sanitización para inyección segura en Javascript
                    String escaped = content.replace("\\", "\\\\")
                                            .replace("\"", "\\\"")
                                            .replace("\n", "\\n")
                                            .replace("\r", "")
                                            .replace("<", "\\u003C")
                                            .replace(">", "\\u003E");
                    jsTextArray.append(", \"").append(escaped).append("\"");
                } else {
                    jsTextArray.append(", \"\"");
                }
            }
            jsTextArray.append("];\n");
            addFileToZip(zos, "assets/js/texts.js", jsTextArray.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // Empaquetar SOLO los archivos JPG visuales al ZIP y notificar progreso
            File[] files = tempDir.listFiles();
            if (files != null) {
                int count = 0;
                int totalImages = 0;
                for (File f : files) { if (f.getName().endsWith(".jpg")) totalImages++; }
                for (File file : files) {
                    if (file.getName().endsWith(".jpg")) {
                        byte[] fileData = Files.readAllBytes(file.toPath());
                        // Usar siempre '/' para los nombres de entrada del ZIP (Estándar ZIP)
                        addFileToZip(zos, "assets/pages/" + file.getName(), fileData);
                        count++;
                        if (progressCallback != null && totalImages > 0) {
                            progressCallback.accept((double) count / totalImages);
                        }
                    }
                }
            }
        }
        
        System.out.println("✅ [Zip SCORM] Arquitectura de visor generada en: " + zipFile.getAbsolutePath());
        return zipFile.getAbsolutePath();
    }
    
    private String readTemplate(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Plantilla no encontrada, asegurate que compiló " + path);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        }
    }
    
    private void addFileToZip(ZipOutputStream zos, String filename, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content, 0, content.length);
        zos.closeEntry();
    }
    
    private String buildManifest(String title) {
        String safeXmlTitle = title != null ? title.replace("&", "&amp;")
                                                   .replace("<", "&lt;")
                                                   .replace(">", "&gt;")
                                                   .replace("\"", "&quot;")
                                                   .replace("'", "&apos;") : "eBook";

        return "<?xml version=\"1.0\" standalone=\"no\" ?>\n" +
               "<manifest identifier=\"com.oliver.scorm\" version=\"1.2\" \n" +
               "          xmlns=\"http://www.imsproject.org/xsd/imscp_rootv1p1p2\" \n" +
               "          xmlns:adlcp=\"http://www.adlnet.org/xsd/adlcp_rootv1p2\">\n" +
               "  <metadata>\n" +
               "    <schema>ADL SCORM</schema>\n" +
               "    <schemaversion>1.2</schemaversion>\n" +
               "  </metadata>\n" +
               "  <organizations default=\"org_1\">\n" +
               "    <organization identifier=\"org_1\">\n" +
               "      <title>" + safeXmlTitle + "</title>\n" +
               "      <item identifier=\"item_1\" identifierref=\"resource_1\">\n" +
               "        <title>" + safeXmlTitle + "</title>\n" +
               "      </item>\n" +
               "    </organization>\n" +
               "  </organizations>\n" +
               "  <resources>\n" +
               "    <resource identifier=\"resource_1\" type=\"webcontent\" href=\"index.html\" adlcp:scormtype=\"sco\">\n" +
               "      <file href=\"index.html\" />\n" +
               "    </resource>\n" +
               "  </resources>\n" +
               "</manifest>";
    }
}
