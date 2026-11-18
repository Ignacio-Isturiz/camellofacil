package com.camellofacil.backend.plantillas.app;

import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.SeccionDocumento;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.Borders;
import org.springframework.stereotype.Component;

/**
 * Convierte el documento estructurado de una hoja de vida a PDF o Word. Todo se arma en memoria: el servidor no
 * guarda archivos. Es la misma estructura que ve la persona en pantalla, con una tipografía acorde a la plantilla
 * (serif para Harvard, sin serifa para ATS).
 */
@Component
public class ExportadorDocumento {

    public enum Formato {
        PDF("application/pdf", "pdf"),
        DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

        private final String tipoContenido;
        private final String extension;

        Formato(String tipoContenido, String extension) {
            this.tipoContenido = tipoContenido;
            this.extension = extension;
        }

        public String tipoContenido() { return tipoContenido; }
        public String extension() { return extension; }

        /** «pdf» o «docx», sin distinguir mayúsculas; cualquier otra cosa es vacío. */
        public static java.util.Optional<Formato> de(String texto) {
            if (texto == null) return java.util.Optional.empty();
            return switch (texto.strip().toLowerCase(Locale.ROOT)) {
                case "pdf" -> java.util.Optional.of(PDF);
                case "docx" -> java.util.Optional.of(DOCX);
                default -> java.util.Optional.empty();
            };
        }
    }

    public record Archivo(byte[] contenido, Formato formato) {}

    private static final float MARGEN = 54f;
    private static final float TAMANO_NOMBRE = 18f;
    private static final float TAMANO_TITULO = 11f;
    private static final float TAMANO_TEXTO = 10.5f;
    private static final float INTERLINEADO = 1.3f;

    public Archivo exportar(DocumentoHoja documento, String estilo, Formato formato) {
        boolean serif = "harvard".equals(estilo);
        try {
            return new Archivo(formato == Formato.PDF ? pdf(documento, serif) : docx(documento, serif), formato);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo armar el archivo", e);
        }
    }

    // ---------- PDF ----------

    private static byte[] pdf(DocumentoHoja d, boolean serif) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDType1Font normal = new PDType1Font(serif ? Standard14Fonts.FontName.TIMES_ROMAN : Standard14Fonts.FontName.HELVETICA);
            PDType1Font negrita = new PDType1Font(serif ? Standard14Fonts.FontName.TIMES_BOLD : Standard14Fonts.FontName.HELVETICA_BOLD);
            Escritor e = new Escritor(doc, normal, negrita);

            if (!d.nombre().isBlank()) e.linea(d.nombre(), negrita, TAMANO_NOMBRE, true, 4);
            if (!d.contacto().isEmpty()) e.lineas(String.join("  ·  ", d.contacto()), normal, TAMANO_TEXTO, true, 10);
            for (SeccionDocumento s : d.secciones()) {
                e.espacio(8);
                e.linea(s.titulo().toUpperCase(Locale.ROOT), negrita, TAMANO_TITULO, false, 2);
                e.regla();
                for (String bloque : s.bloques()) {
                    e.lineas(bloque, normal, TAMANO_TEXTO, false, 5);
                }
            }
            e.cerrar();
            doc.save(salida);
            return salida.toByteArray();
        }
    }

    /** Escribe líneas de texto en páginas A4, saltando de página cuando hace falta. */
    private static final class Escritor {
        private final PDDocument doc;
        private final PDType1Font normal;
        private final PDType1Font negrita;
        private PDPageContentStream flujo;
        private float y;

        Escritor(PDDocument doc, PDType1Font normal, PDType1Font negrita) throws IOException {
            this.doc = doc;
            this.normal = normal;
            this.negrita = negrita;
            nuevaPagina();
        }

        private void nuevaPagina() throws IOException {
            if (flujo != null) flujo.close();
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);
            flujo = new PDPageContentStream(doc, pagina);
            y = pagina.getMediaBox().getHeight() - MARGEN;
        }

        private float ancho() {
            return PDRectangle.A4.getWidth() - 2 * MARGEN;
        }

        void espacio(float puntos) {
            y -= puntos;
        }

        void linea(String texto, PDType1Font fuente, float tamano, boolean centrado, float despues) throws IOException {
            escribir(limpiar(texto, fuente), fuente, tamano, centrado);
            y -= despues;
        }

        void lineas(String texto, PDType1Font fuente, float tamano, boolean centrado, float despues) throws IOException {
            for (String linea : partir(limpiar(texto, fuente), fuente, tamano, ancho())) {
                escribir(linea, fuente, tamano, centrado);
            }
            y -= despues;
        }

        void regla() throws IOException {
            if (y < MARGEN + 20) nuevaPagina();
            flujo.setLineWidth(0.5f);
            flujo.moveTo(MARGEN, y + 2);
            flujo.lineTo(MARGEN + ancho(), y + 2);
            flujo.stroke();
            y -= 6;
        }

        private void escribir(String linea, PDType1Font fuente, float tamano, boolean centrado) throws IOException {
            float alto = tamano * INTERLINEADO;
            if (y - alto < MARGEN) nuevaPagina();
            y -= alto;
            float x = MARGEN;
            if (centrado) x += Math.max(0, (ancho() - anchoDe(linea, fuente, tamano)) / 2);
            flujo.beginText();
            flujo.setFont(fuente, tamano);
            flujo.newLineAtOffset(x, y);
            flujo.showText(linea);
            flujo.endText();
        }

        void cerrar() throws IOException {
            flujo.close();
        }
    }

    private static float anchoDe(String texto, PDType1Font fuente, float tamano) throws IOException {
        return fuente.getStringWidth(texto) / 1000f * tamano;
    }

    /** Parte el texto en líneas que quepan en el ancho; una palabra más larga que la línea se corta. */
    private static List<String> partir(String texto, PDType1Font fuente, float tamano, float ancho) throws IOException {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            if (palabra.isEmpty()) continue;
            while (anchoDe(palabra, fuente, tamano) > ancho) {
                int corte = palabra.length();
                while (corte > 1 && anchoDe(palabra.substring(0, corte), fuente, tamano) > ancho) corte--;
                if (actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual.setLength(0);
                }
                lineas.add(palabra.substring(0, corte));
                palabra = palabra.substring(corte);
            }
            String candidata = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (anchoDe(candidata, fuente, tamano) <= ancho) {
                actual.setLength(0);
                actual.append(candidata);
            } else {
                lineas.add(actual.toString());
                actual.setLength(0);
                actual.append(palabra);
            }
        }
        if (actual.length() > 0) lineas.add(actual.toString());
        return lineas;
    }

    /** Deja solo lo que la fuente estándar del PDF sabe escribir (Latin-1 y puntuación común); lo demás pasa a «?». */
    private static String limpiar(String texto, PDType1Font fuente) {
        StringBuilder s = new StringBuilder();
        texto.codePoints().forEach(cp -> {
            if (Character.isISOControl(cp) || Character.isWhitespace(cp)) {
                s.append(' ');
                return;
            }
            String c = new String(Character.toChars(cp));
            try {
                fuente.encode(c);
                s.append(c);
            } catch (IOException | IllegalArgumentException e) {
                s.append('?');
            }
        });
        return s.toString().replaceAll(" {2,}", " ").strip();
    }

    // ---------- Word ----------

    private static byte[] docx(DocumentoHoja d, boolean serif) throws IOException {
        String fuente = serif ? "Times New Roman" : "Arial";
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            if (!d.nombre().isBlank()) {
                parrafo(doc, d.nombre(), fuente, 18, true, ParagraphAlignment.CENTER, 60);
            }
            if (!d.contacto().isEmpty()) {
                parrafo(doc, String.join("  ·  ", d.contacto()), fuente, 10, false, ParagraphAlignment.CENTER, 200);
            }
            for (SeccionDocumento s : d.secciones()) {
                XWPFParagraph titulo = parrafo(doc, s.titulo().toUpperCase(Locale.ROOT), fuente, 11, true, ParagraphAlignment.LEFT, 80);
                titulo.setBorderBottom(Borders.SINGLE);
                titulo.setSpacingBefore(160);
                for (String bloque : s.bloques()) {
                    parrafo(doc, bloque, fuente, 10.5, false, ParagraphAlignment.LEFT, 100);
                }
            }
            doc.write(salida);
            return salida.toByteArray();
        }
    }

    private static XWPFParagraph parrafo(
            XWPFDocument doc, String texto, String fuente, double tamano, boolean negrita, ParagraphAlignment alineacion, int despues) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(alineacion);
        p.setSpacingAfter(despues);
        XWPFRun r = p.createRun();
        r.setText(texto.replaceAll("\\p{Cntrl}", " "));
        r.setFontFamily(fuente);
        r.setFontSize(tamano);
        r.setBold(negrita);
        return p;
    }
}
