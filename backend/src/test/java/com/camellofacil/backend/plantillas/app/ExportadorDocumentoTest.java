package com.camellofacil.backend.plantillas.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.camellofacil.backend.plantillas.app.ExportadorDocumento.Archivo;
import com.camellofacil.backend.plantillas.app.ExportadorDocumento.Formato;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.SeccionDocumento;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/** El PDF y el Word se leen de vuelta con las mismas librerías que usa el análisis de hojas. */
class ExportadorDocumentoTest {

    private final ExportadorDocumento exportador = new ExportadorDocumento();

    private static DocumentoHoja documento(List<String> bloquesExperiencia) {
        return new DocumentoHoja("Ana Prueba Ficticia", List.of("ana@unaula.edu.co", "300 123 4567", "Medellín"), List.of(
                new SeccionDocumento("perfil", "Perfil", List.of("Estudiante de datos con interés en análisis.")),
                new SeccionDocumento("experiencia", "Experiencia", bloquesExperiencia)));
    }

    private static String textoPdf(byte[] bytes) throws Exception {
        try (PDDocument d = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(d);
        }
    }

    private static String textoDocx(byte[] bytes) throws Exception {
        try (XWPFDocument d = new XWPFDocument(new ByteArrayInputStream(bytes)); XWPFWordExtractor e = new XWPFWordExtractor(d)) {
            return e.getText();
        }
    }

    @Test
    void elPdfLlevaTodoElContenidoConTildes() throws Exception {
        Archivo a = exportador.exportar(documento(List.of("Monitora — Universidad de Ejemplo (2024-02 – Actual). Apoyé prácticas.")), "harvard", Formato.PDF);

        assertThat(new String(a.contenido(), 0, 4)).isEqualTo("%PDF");
        String texto = textoPdf(a.contenido());
        assertThat(texto).contains("Ana Prueba Ficticia", "Medellín", "PERFIL", "EXPERIENCIA", "Monitora", "Apoyé prácticas");
    }

    @Test
    void elWordLlevaTodoElContenido() throws Exception {
        Archivo a = exportador.exportar(documento(List.of("Monitora — Universidad de Ejemplo (2024-02 – Actual).")), "ats-clasico", Formato.DOCX);

        assertThat(new String(a.contenido(), 0, 2)).isEqualTo("PK");
        assertThat(textoDocx(a.contenido())).contains("Ana Prueba Ficticia", "300 123 4567", "EXPERIENCIA", "Monitora");
    }

    @Test
    void unDocumentoLargoPasaAVariasPaginas() throws Exception {
        List<String> muchos = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            muchos.add("Cargo " + i + " — Organización de ejemplo (2020-01 – 2021-01). Descripción larga de lo que hizo la persona en ese puesto, "
                    + "con varias frases para que el párrafo ocupe más de una línea dentro de la página.");
        }
        Archivo a = exportador.exportar(documento(muchos), "ats-moderno", Formato.PDF);

        try (PDDocument d = Loader.loadPDF(a.contenido())) {
            assertThat(d.getNumberOfPages()).isGreaterThan(1);
        }
        assertThat(textoPdf(a.contenido())).contains("Cargo 0", "Cargo 79");
    }

    @Test
    void loQueLaFuenteDelPdfNoSabeEscribirSeReemplazaSinRomperElArchivo() throws Exception {
        String conEmojiYPalabraEnorme = "Lideré 😀 el equipo " + "x".repeat(400);
        Archivo a = exportador.exportar(documento(List.of(conEmojiYPalabraEnorme)), "ats-compacto", Formato.PDF);

        String texto = textoPdf(a.contenido());
        assertThat(texto).contains("Lideré", "?", "xxxxxxxxxx");
    }

    @Test
    void tambienSirveSinNombreNiContacto() throws Exception {
        DocumentoHoja vacio = new DocumentoHoja("", List.of(), List.of());
        assertThat(exportador.exportar(vacio, "harvard", Formato.PDF).contenido()).isNotEmpty();
        assertThat(exportador.exportar(vacio, "harvard", Formato.DOCX).contenido()).isNotEmpty();
    }

    @Test
    void elFormatoSeLeeSinDistinguirMayusculasYRechazaElResto() {
        assertThat(Formato.de("PDF")).contains(Formato.PDF);
        assertThat(Formato.de(" docx ")).contains(Formato.DOCX);
        assertThat(Formato.de("exe")).isEmpty();
        assertThat(Formato.de(null)).isEmpty();
    }
}
