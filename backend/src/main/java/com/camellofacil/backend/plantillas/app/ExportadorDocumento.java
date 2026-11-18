package com.camellofacil.backend.plantillas.app;

import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.EntradaDocumento;
import com.camellofacil.backend.plantillas.app.ModelosPlantilla.SeccionDocumento;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabStop;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTabJc;
import org.springframework.stereotype.Component;

/**
 * Convierte el documento estructurado de una hoja de vida a PDF o Word. Todo se arma en memoria: el servidor no
 * guarda archivos. Cada plantilla tiene su tema (tipografía, márgenes y color); las experiencias y estudios se
 * acomodan con el título a la izquierda, las fechas alineadas a la derecha, el lugar debajo y los logros en viñetas.
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
        public static Optional<Formato> de(String texto) {
            if (texto == null) return Optional.empty();
            return switch (texto.strip().toLowerCase(Locale.ROOT)) {
                case "pdf" -> Optional.of(PDF);
                case "docx" -> Optional.of(DOCX);
                default -> Optional.empty();
            };
        }
    }

    public record Archivo(byte[] contenido, Formato formato) {}

    /** Aspecto de una plantilla. Los tamaños están en puntos; el color es RGB (0-255). */
    record Tema(
            boolean serif, float margen, float tamanoNombre, float tamanoTexto, float tamanoTitulo, int[] acento,
            boolean centrado, float espacioSeccion, float espacioEntrada) {

        static Tema de(String estilo) {
            return switch (estilo == null ? "" : estilo) {
                case "harvard" -> new Tema(true, 60, 21, 10.5f, 11, new int[] {0, 0, 0}, true, 13, 6);
                case "ats-moderno" -> new Tema(false, 56, 22, 10, 10.5f, new int[] {158, 27, 27}, false, 14, 7);
                case "ats-compacto" -> new Tema(false, 42, 18, 9.5f, 10, new int[] {0, 0, 0}, false, 9, 4);
                default -> new Tema(false, 56, 20, 10, 10.5f, new int[] {0, 0, 0}, false, 12, 6);
            };
        }
    }

    private static final float INTERLINEADO = 1.32f;

    public Archivo exportar(DocumentoHoja documento, String estilo, Formato formato) {
        Tema tema = Tema.de(estilo);
        try {
            return new Archivo(formato == Formato.PDF ? pdf(documento, tema) : docx(documento, tema), formato);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo armar el archivo", e);
        }
    }

    // ---------- Contenido común ----------

    /**
     * El detalle de un cargo en viñetas: una por línea, o una por oración si hay varias. Si es una sola frase se
     * deja como párrafo (lista vacía y el texto completo en `parrafo`).
     */
    record Detalle(List<String> vinetas, String parrafo) {}

    static Detalle partirDetalle(String texto) {
        if (texto == null || texto.isBlank()) return new Detalle(List.of(), "");
        List<String> partes = new ArrayList<>();
        for (String linea : texto.strip().split("\\R+")) {
            String l = linea.strip().replaceFirst("^[-•*·]\\s*", "");
            if (!l.isEmpty()) partes.add(l);
        }
        if (partes.size() == 1) {
            // Una sola línea: se parte por oraciones si cada una empieza con mayúscula.
            partes = new ArrayList<>(List.of(partes.get(0).split("(?<=[.!?])\\s+(?=\\p{Lu})")));
        }
        if (partes.size() <= 1) return new Detalle(List.of(), partes.isEmpty() ? "" : partes.get(0));
        return new Detalle(partes, "");
    }

    private static boolean hayTexto(String s) {
        return s != null && !s.isBlank();
    }

    // ---------- PDF ----------

    private static byte[] pdf(DocumentoHoja d, Tema t) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Fuentes f = Fuentes.de(t.serif());
            Pagina p = new Pagina(doc, t, f);

            if (hayTexto(d.nombre())) {
                p.linea(d.nombre(), f.negrita(), t.tamanoNombre(), t.centrado(), t.acento());
                p.espacio(2);
            }
            if (!d.contacto().isEmpty()) {
                p.parrafo(String.join("   |   ", d.contacto()), f.normal(), t.tamanoTexto() - 0.5f, 0, t.centrado(), GRIS);
            }
            for (SeccionDocumento s : d.secciones()) {
                p.titulo(s.titulo());
                if (!s.entradas().isEmpty()) {
                    for (EntradaDocumento e : s.entradas()) p.entrada(e);
                } else {
                    for (String bloque : s.bloques()) {
                        p.parrafo(bloque, f.normal(), t.tamanoTexto(), 0, false, NEGRO);
                        p.espacio(t.espacioEntrada() - 2);
                    }
                }
            }
            p.cerrar();
            doc.save(salida);
            return salida.toByteArray();
        }
    }

    private static final int[] NEGRO = {20, 20, 20};
    private static final int[] GRIS = {85, 85, 85};

    private record Fuentes(PDType1Font normal, PDType1Font negrita, PDType1Font cursiva) {
        static Fuentes de(boolean serif) {
            return serif
                    ? new Fuentes(new PDType1Font(FontName.TIMES_ROMAN), new PDType1Font(FontName.TIMES_BOLD), new PDType1Font(FontName.TIMES_ITALIC))
                    : new Fuentes(new PDType1Font(FontName.HELVETICA), new PDType1Font(FontName.HELVETICA_BOLD), new PDType1Font(FontName.HELVETICA_OBLIQUE));
        }
    }

    /**
     * Escribe en páginas A4 llevando la cuenta de la posición vertical. Antes de escribir cualquier línea comprueba
     * que quepa; los títulos de sección y el encabezado de cada entrada viajan junto a su primera línea de texto para
     * que no queden solos al final de una página.
     */
    private static final class Pagina {
        private final PDDocument doc;
        private final Tema tema;
        private final Fuentes f;
        private PDPageContentStream flujo;
        private float y;

        Pagina(PDDocument doc, Tema tema, Fuentes f) throws IOException {
            this.doc = doc;
            this.tema = tema;
            this.f = f;
            nueva();
        }

        private float ancho() {
            return PDRectangle.A4.getWidth() - 2 * tema.margen();
        }

        private void nueva() throws IOException {
            if (flujo != null) flujo.close();
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);
            flujo = new PDPageContentStream(doc, pagina);
            y = pagina.getMediaBox().getHeight() - tema.margen();
        }

        void cerrar() throws IOException {
            flujo.close();
        }

        void espacio(float puntos) {
            y -= puntos;
        }

        /** Salta de página si no caben `alto` puntos. */
        private void asegurar(float alto) throws IOException {
            if (y - alto < tema.margen()) nueva();
        }

        private static float alto(float tamano) {
            return tamano * INTERLINEADO;
        }

        /** Una línea (el texto no se parte: se usa para el nombre y los títulos). */
        void linea(String texto, PDType1Font fuente, float tamano, boolean centrado, int[] color) throws IOException {
            String limpio = limpiar(texto, fuente);
            asegurar(alto(tamano));
            y -= alto(tamano);
            float x = tema.margen();
            if (centrado) x += Math.max(0, (ancho() - anchoDe(limpio, fuente, tamano)) / 2);
            escribir(limpio, fuente, tamano, x, y, color);
        }

        /** Un párrafo que se parte en líneas; `sangria` lo desplaza a la derecha (para las viñetas). */
        void parrafo(String texto, PDType1Font fuente, float tamano, float sangria, boolean centrado, int[] color) throws IOException {
            for (String l : partir(limpiar(texto, fuente), fuente, tamano, ancho() - sangria)) {
                asegurar(alto(tamano));
                y -= alto(tamano);
                float x = tema.margen() + sangria;
                if (centrado) x += Math.max(0, (ancho() - sangria - anchoDe(l, fuente, tamano)) / 2);
                escribir(l, fuente, tamano, x, y, color);
            }
        }

        void titulo(String texto) throws IOException {
            float tamano = tema.tamanoTitulo();
            // El título y al menos dos líneas de texto caben juntos, o todo pasa a la página siguiente.
            asegurar(tema.espacioSeccion() + alto(tamano) + 8 + 2 * alto(tema.tamanoTexto()));
            y -= tema.espacioSeccion();
            String limpio = limpiar(texto, f.negrita()).toUpperCase(Locale.ROOT);
            y -= alto(tamano);
            float x = tema.margen();
            if (tema.centrado()) x += Math.max(0, (ancho() - anchoDe(limpio, f.negrita(), tamano)) / 2);
            escribir(limpio, f.negrita(), tamano, x, y, tema.acento());
            // La regla va debajo del título, separada de las letras (nunca las atraviesa).
            y -= 4;
            flujo.setStrokingColor(tema.acento()[0] / 255f, tema.acento()[1] / 255f, tema.acento()[2] / 255f);
            flujo.setLineWidth(tema.acento()[0] == 0 && tema.acento()[1] == 0 ? 0.6f : 1.1f);
            flujo.moveTo(tema.margen(), y);
            flujo.lineTo(tema.margen() + ancho(), y);
            flujo.stroke();
            y -= 6;
        }

        /** Cargo (o estudio) con las fechas a la derecha, el lugar debajo y el detalle. */
        void entrada(EntradaDocumento e) throws IOException {
            float tamano = tema.tamanoTexto();
            asegurar(2 * alto(tamano) + (hayTexto(e.lugar()) ? alto(tamano) : 0));
            String titulo = limpiar(hayTexto(e.titulo()) ? e.titulo() : e.lugar(), f.negrita());
            String fechas = limpiar(e.fechas() == null ? "" : e.fechas(), f.normal());
            float anchoFechas = fechas.isEmpty() ? 0 : anchoDe(fechas, f.normal(), tamano);
            float disponible = ancho() - (fechas.isEmpty() ? 0 : anchoFechas + 14);

            List<String> lineas = partir(titulo, f.negrita(), tamano, disponible);
            for (int i = 0; i < lineas.size(); i++) {
                asegurar(alto(tamano));
                y -= alto(tamano);
                escribir(lineas.get(i), f.negrita(), tamano, tema.margen(), y, NEGRO);
                if (i == 0 && !fechas.isEmpty()) {
                    escribir(fechas, f.normal(), tamano, tema.margen() + ancho() - anchoFechas, y, GRIS);
                }
            }
            if (hayTexto(e.titulo()) && hayTexto(e.lugar())) {
                parrafo(e.lugar(), f.cursiva(), tamano, 0, false, GRIS);
            }
            Detalle det = partirDetalle(e.detalle());
            if (!det.vinetas().isEmpty()) {
                espacio(1.5f);
                for (String v : det.vinetas()) vineta(v, tamano);
            } else if (!det.parrafo().isEmpty()) {
                espacio(1.5f);
                parrafo(det.parrafo(), f.normal(), tamano, 0, false, NEGRO);
            }
            espacio(tema.espacioEntrada());
        }

        private void vineta(String texto, float tamano) throws IOException {
            float sangria = 11;
            List<String> lineas = partir(limpiar(texto, f.normal()), f.normal(), tamano, ancho() - sangria);
            for (int i = 0; i < lineas.size(); i++) {
                asegurar(alto(tamano));
                y -= alto(tamano);
                if (i == 0) escribir("•", f.normal(), tamano, tema.margen() + 2, y, NEGRO);
                escribir(lineas.get(i), f.normal(), tamano, tema.margen() + sangria, y, NEGRO);
            }
        }

        private void escribir(String texto, PDType1Font fuente, float tamano, float x, float base, int[] color) throws IOException {
            flujo.beginText();
            flujo.setFont(fuente, tamano);
            flujo.setNonStrokingColor(color[0] / 255f, color[1] / 255f, color[2] / 255f);
            flujo.newLineAtOffset(x, base);
            flujo.showText(texto);
            flujo.endText();
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

    private static final int PAGINA_ANCHO = 11906; // A4 en veinteavos de punto
    private static final int PAGINA_ALTO = 16838;

    private static byte[] docx(DocumentoHoja d, Tema t) throws IOException {
        String fuente = t.serif() ? "Times New Roman" : "Arial";
        String acento = String.format("%02X%02X%02X", t.acento()[0], t.acento()[1], t.acento()[2]);
        int margen = Math.round(t.margen() * 20);
        int anchoUtil = PAGINA_ANCHO - 2 * margen;
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            paginaA4(doc, margen);
            ParagraphAlignment alineacion = t.centrado() ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT;

            if (hayTexto(d.nombre())) {
                XWPFParagraph p = parrafoWord(doc, alineacion, 40, 0);
                corrida(p, d.nombre().strip(), fuente, t.tamanoNombre(), true, false, acento);
            }
            if (!d.contacto().isEmpty()) {
                XWPFParagraph p = parrafoWord(doc, alineacion, 60, 0);
                corrida(p, String.join("   |   ", d.contacto()), fuente, t.tamanoTexto() - 0.5, false, false, "555555");
            }
            for (SeccionDocumento s : d.secciones()) {
                XWPFParagraph titulo = parrafoWord(doc, alineacion, 80, Math.round(t.espacioSeccion() * 20));
                titulo.setKeepNext(true);
                corrida(titulo, s.titulo().toUpperCase(Locale.ROOT), fuente, t.tamanoTitulo(), true, false, acento);
                reglaWord(titulo, acento);
                if (!s.entradas().isEmpty()) {
                    for (EntradaDocumento e : s.entradas()) entradaWord(doc, e, fuente, t, anchoUtil);
                } else {
                    for (String bloque : s.bloques()) {
                        corrida(parrafoWord(doc, ParagraphAlignment.LEFT, 100, 0), bloque, fuente, t.tamanoTexto(), false, false, "141414");
                    }
                }
            }
            doc.write(salida);
            return salida.toByteArray();
        }
    }

    private static void entradaWord(XWPFDocument doc, EntradaDocumento e, String fuente, Tema t, int anchoUtil) {
        String titulo = hayTexto(e.titulo()) ? e.titulo().strip() : e.lugar().strip();
        XWPFParagraph cabecera = parrafoWord(doc, ParagraphAlignment.LEFT, 0, 60);
        cabecera.setKeepNext(true);
        // Tabulación a la derecha: las fechas quedan alineadas al borde, como en el PDF.
        CTTabStop tab = cabecera.getCTP().addNewPPr().addNewTabs().addNewTab();
        tab.setVal(STTabJc.RIGHT);
        tab.setPos(BigInteger.valueOf(anchoUtil));
        XWPFRun r = corrida(cabecera, titulo, fuente, t.tamanoTexto(), true, false, "141414");
        if (hayTexto(e.fechas())) {
            r.addTab();
            corrida(cabecera, e.fechas(), fuente, t.tamanoTexto(), false, false, "555555");
        }
        if (hayTexto(e.titulo()) && hayTexto(e.lugar())) {
            XWPFParagraph lugar = parrafoWord(doc, ParagraphAlignment.LEFT, 30, 0);
            lugar.setKeepNext(hayTexto(e.detalle()));
            corrida(lugar, e.lugar().strip(), fuente, t.tamanoTexto(), false, true, "555555");
        }
        Detalle det = partirDetalle(e.detalle());
        if (!det.vinetas().isEmpty()) {
            for (String v : det.vinetas()) {
                XWPFParagraph p = parrafoWord(doc, ParagraphAlignment.LEFT, 30, 0);
                p.setIndentationLeft(280);
                p.setIndentationHanging(200);
                corrida(p, "•  " + v, fuente, t.tamanoTexto(), false, false, "141414");
            }
        } else if (!det.parrafo().isEmpty()) {
            corrida(parrafoWord(doc, ParagraphAlignment.LEFT, 30, 0), det.parrafo(), fuente, t.tamanoTexto(), false, false, "141414");
        }
        // Espacio entre una entrada y la siguiente.
        XWPFParagraph ultima = doc.getParagraphs().get(doc.getParagraphs().size() - 1);
        ultima.setSpacingAfter(Math.round(t.espacioEntrada() * 20));
    }

    private static void paginaA4(XWPFDocument doc, int margen) {
        CTSectPr sect = doc.getDocument().getBody().isSetSectPr() ? doc.getDocument().getBody().getSectPr() : doc.getDocument().getBody().addNewSectPr();
        CTPageSz tamano = sect.isSetPgSz() ? sect.getPgSz() : sect.addNewPgSz();
        tamano.setW(BigInteger.valueOf(PAGINA_ANCHO));
        tamano.setH(BigInteger.valueOf(PAGINA_ALTO));
        CTPageMar m = sect.isSetPgMar() ? sect.getPgMar() : sect.addNewPgMar();
        m.setTop(BigInteger.valueOf(margen));
        m.setBottom(BigInteger.valueOf(margen));
        m.setLeft(BigInteger.valueOf(margen));
        m.setRight(BigInteger.valueOf(margen));
    }

    private static XWPFParagraph parrafoWord(XWPFDocument doc, ParagraphAlignment alineacion, int despues, int antes) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(alineacion);
        p.setSpacingAfter(despues);
        p.setSpacingBefore(antes);
        return p;
    }

    private static XWPFRun corrida(XWPFParagraph p, String texto, String fuente, double tamano, boolean negrita, boolean cursiva, String color) {
        XWPFRun r = p.createRun();
        r.setText(texto.replaceAll("\\p{Cntrl}", " "));
        r.setFontFamily(fuente);
        r.setFontSize(tamano);
        r.setBold(negrita);
        r.setItalic(cursiva);
        r.setColor(color);
        return r;
    }

    /** Línea fina bajo el título de la sección, del color del tema. */
    private static void reglaWord(XWPFParagraph p, String color) {
        p.setBorderBottom(Borders.SINGLE);
        var borde = p.getCTP().getPPr().getPBdr().getBottom();
        borde.setColor(color);
        borde.setSz(BigInteger.valueOf(6));
        borde.setSpace(BigInteger.valueOf(2));
    }
}
