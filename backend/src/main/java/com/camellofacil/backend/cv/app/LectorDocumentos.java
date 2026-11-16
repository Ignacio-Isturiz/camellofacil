package com.camellofacil.backend.cv.app;

import com.camellofacil.backend.cv.app.ModelosHoja.TipoArchivo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

/**
 * Reconoce el tipo real del archivo por su contenido (no por la extensión) y saca su texto en nuestro servidor.
 * Al proveedor de IA solo viaja el texto, nunca el archivo.
 */
@Component
public class LectorDocumentos {

    /** Por debajo de esto no hay texto suficiente para una hoja de vida (p. ej. un PDF escaneado). */
    static final int MINIMO_CARACTERES = 150;

    private static final byte[] FIRMA_PDF = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] FIRMA_ZIP = {'P', 'K', 3, 4};
    private static final byte[] FIRMA_OLE = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};

    public record DocumentoLeido(String texto, int paginas) {}

    /** Thrown cuando el archivo no se puede leer o casi no tiene texto. */
    public static class ArchivoIlegibleException extends Exception {
        public ArchivoIlegibleException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }

    public Optional<TipoArchivo> detectarTipo(byte[] bytes) {
        if (empiezaCon(bytes, FIRMA_PDF)) return Optional.of(TipoArchivo.PDF);
        // Un .docx es un ZIP; que de verdad sea Word lo confirma POI al leerlo.
        if (empiezaCon(bytes, FIRMA_ZIP)) return Optional.of(TipoArchivo.DOCX);
        if (empiezaCon(bytes, FIRMA_OLE)) return Optional.of(TipoArchivo.DOC);
        return Optional.empty();
    }

    public DocumentoLeido leer(byte[] bytes, TipoArchivo tipo) throws ArchivoIlegibleException {
        DocumentoLeido leido;
        try {
            leido = switch (tipo) {
                case PDF -> leerPdf(bytes);
                case DOCX -> leerDocx(bytes);
                case DOC -> leerDoc(bytes);
            };
        } catch (IOException | RuntimeException e) {
            throw new ArchivoIlegibleException("No se pudo leer el archivo", e);
        }
        String texto = leido.texto().replace('\u0000', ' ').strip();
        if (texto.length() < MINIMO_CARACTERES) {
            throw new ArchivoIlegibleException("El archivo casi no tiene texto", null);
        }
        return new DocumentoLeido(texto, Math.max(1, leido.paginas()));
    }

    private static DocumentoLeido leerPdf(byte[] bytes) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            return new DocumentoLeido(new PDFTextStripper().getText(pdf), pdf.getNumberOfPages());
        }
    }

    private static DocumentoLeido leerDocx(byte[] bytes) throws IOException {
        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(docx)) {
            int paginas = docx.getProperties().getExtendedProperties().getPages();
            return new DocumentoLeido(extractor.getText(), paginas);
        }
    }

    private static DocumentoLeido leerDoc(byte[] bytes) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(doc)) {
            int paginas = doc.getSummaryInformation() == null ? 1 : doc.getSummaryInformation().getPageCount();
            return new DocumentoLeido(extractor.getText(), paginas);
        }
    }

    private static boolean empiezaCon(byte[] bytes, byte[] firma) {
        return bytes.length >= firma.length && Arrays.equals(bytes, 0, firma.length, firma, 0, firma.length);
    }
}
