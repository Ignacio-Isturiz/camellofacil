package com.camellofacil.backend.plantillas.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.camellofacil.backend.plantillas.app.ExportadorDocumento.Archivo;
import com.camellofacil.backend.plantillas.app.ExportadorDocumento.Formato;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.EntradaDocumento;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.SeccionDocumento;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
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

    // ---------- Diseño: nada se sobrepone y todo queda dentro de la hoja ----------

    private static DocumentoHoja conEntradas(int cuantas) {
        List<EntradaDocumento> entradas = new ArrayList<>();
        for (int i = 0; i < cuantas; i++) {
            entradas.add(new EntradaDocumento("Cargo " + i, "Organización " + i, "ene 2024 – Actual",
                    "Primer logro de la persona en este puesto. Segundo logro con 30% de mejora. Tercer logro bastante más largo para que ocupe "
                            + "más de una línea dentro de la página y se compruebe el ajuste de las viñetas."));
        }
        return new DocumentoHoja("Ana Prueba Ficticia", List.of("ana@unaula.edu.co", "300 123 4567"), List.of(
                new SeccionDocumento("perfil", "Perfil", List.of("Estudiante de datos."), List.of()),
                new SeccionDocumento("experiencia", "Experiencia", List.of(), entradas),
                new SeccionDocumento("habilidades", "Habilidades", List.of("Python, SQL, Excel"), List.of())));
    }

    /** Cada línea del PDF con su página, su posición vertical y su tamaño de letra. */
    private record Linea(int pagina, float y, float tamano, float xFin, String texto) {}

    private static List<Linea> lineas(byte[] pdf) throws Exception {
        List<Linea> resultado = new ArrayList<>();
        try (PDDocument d = Loader.loadPDF(pdf)) {
            PDFTextStripper lector = new PDFTextStripper() {
                @Override
                protected void writeString(String texto, List<TextPosition> posiciones) {
                    TextPosition ultima = posiciones.get(posiciones.size() - 1);
                    resultado.add(new Linea(getCurrentPageNo(), posiciones.get(0).getYDirAdj(), posiciones.get(0).getFontSizeInPt(),
                            ultima.getXDirAdj() + ultima.getWidthDirAdj(), texto));
                }
            };
            lector.getText(d);
        }
        return resultado;
    }

    @Test
    void lasLineasNoSeSobreponenNiSeSalenDeLaHoja_enLasCuatroPlantillas() throws Exception {
        for (String estilo : List.of("harvard", "ats-clasico", "ats-moderno", "ats-compacto")) {
            // 14 entradas obligan a pasar de página.
            List<Linea> todas = lineas(exportador.exportar(conEntradas(14), estilo, Formato.PDF).contenido());
            assertThat(todas.stream().mapToInt(Linea::pagina).max().orElse(0)).as(estilo).isGreaterThan(1);

            for (int pagina = 1; pagina <= todas.stream().mapToInt(Linea::pagina).max().orElse(0); pagina++) {
                final int pg = pagina;
                List<Linea> deLaPagina = todas.stream().filter(l -> l.pagina() == pg).sorted(java.util.Comparator.comparing(Linea::y)).toList();
                for (int i = 1; i < deLaPagina.size(); i++) {
                    float salto = deLaPagina.get(i).y() - deLaPagina.get(i - 1).y();
                    // Misma línea (título y fechas) o, si no, separadas por más que el alto de la letra.
                    assertThat(salto < 1f || salto >= deLaPagina.get(i).tamano() * 1.05f)
                            .as("%s pág. %d: «%s» pisa a «%s»", estilo, pg, deLaPagina.get(i).texto(), deLaPagina.get(i - 1).texto()).isTrue();
                }
                // Dentro del área escrita: ni cortado abajo ni fuera del margen derecho.
                assertThat(deLaPagina).as(estilo).allSatisfy(l -> {
                    assertThat(l.y()).isLessThan(842f - 30f);
                    assertThat(l.xFin()).isLessThanOrEqualTo(595.3f - 30f);
                });
            }
        }
    }

    @Test
    void lasFechasQuedanALaDerechaDelCargoYLosLogrosVanEnVinetas() throws Exception {
        byte[] pdf = exportador.exportar(conEntradas(1), "ats-clasico", Formato.PDF).contenido();
        List<Linea> l = lineas(pdf);

        Linea cargo = l.stream().filter(x -> x.texto().contains("Cargo 0")).findFirst().orElseThrow();
        Linea fechas = l.stream().filter(x -> x.texto().contains("ene 2024 – Actual")).findFirst().orElseThrow();
        // Misma línea y las fechas más a la derecha que el cargo (alineadas al borde).
        assertThat(Math.abs(fechas.y() - cargo.y())).isLessThan(1f);
        assertThat(fechas.xFin()).isGreaterThan(cargo.xFin() + 100f);
        assertThat(textoPdf(pdf)).contains("• Primer logro", "• Segundo logro", "• Tercer logro");
    }

    @Test
    void unTituloDeSeccionNuncaQuedaSoloAlFinalDeLaPagina() throws Exception {
        for (int cuantas = 5; cuantas <= 16; cuantas++) {
            List<Linea> todas = lineas(exportador.exportar(conEntradas(cuantas), "harvard", Formato.PDF).contenido());
            for (int i = 0; i < todas.size() - 1; i++) {
                Linea l = todas.get(i);
                if (l.texto().equals("HABILIDADES") || l.texto().equals("EXPERIENCIA")) {
                    assertThat(todas.get(i + 1).pagina()).as("%d entradas: «%s» solo al final", cuantas, l.texto()).isEqualTo(l.pagina());
                }
            }
        }
    }

    @Test
    void elWordTraeLasFechasAlineadasLosLogrosYElTamanoA4() throws Exception {
        byte[] docx = exportador.exportar(conEntradas(1), "ats-moderno", Formato.DOCX).contenido();
        try (XWPFDocument d = new XWPFDocument(new ByteArrayInputStream(docx))) {
            var seccion = d.getDocument().getBody().getSectPr();
            assertThat(Integer.parseInt(String.valueOf(seccion.getPgSz().getW()))).isEqualTo(11906);
            assertThat(Integer.parseInt(String.valueOf(seccion.getPgMar().getLeft()))).isEqualTo(1120);
            String xml = d.getDocument().xmlText();
            assertThat(xml).contains("w:val=\"right\"").contains("9E1B1B");
            assertThat(d.getParagraphs().stream().map(p -> p.getText())).anyMatch(t -> t.startsWith("•  Primer logro"));
            assertThat(d.getParagraphs().stream().map(p -> p.getText())).anyMatch(t -> t.contains("Cargo 0") && t.contains("ene 2024 – Actual"));
        }
    }

    @Test
    void partirDetalle_separaPorLineasOPorOraciones() {
        assertThat(ExportadorDocumento.partirDetalle("Hice A. Hice B. Hice C.").vinetas()).containsExactly("Hice A.", "Hice B.", "Hice C.");
        assertThat(ExportadorDocumento.partirDetalle("- uno\n• dos\ntres").vinetas()).containsExactly("uno", "dos", "tres");
        assertThat(ExportadorDocumento.partirDetalle("Una sola frase sin más.").parrafo()).isEqualTo("Una sola frase sin más.");
        assertThat(ExportadorDocumento.partirDetalle("Usé v1.2 y python.org en el proyecto").vinetas()).isEmpty();
        assertThat(ExportadorDocumento.partirDetalle(" ").parrafo()).isEmpty();
    }
}
