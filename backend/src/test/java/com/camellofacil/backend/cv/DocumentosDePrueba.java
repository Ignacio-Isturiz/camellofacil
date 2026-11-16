package com.camellofacil.backend.cv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/** Genera hojas de vida ficticias (PDF y Word) para las pruebas. Nunca datos reales. */
public final class DocumentosDePrueba {

    public static final String[] LINEAS = {
        "Ana Prueba Ficticia",
        "Telefono 300 123 4567 - Medellin",
        "Perfil: estudiante de ingenieria con interes en analisis de datos y visualizacion.",
        "Experiencia: Monitora de laboratorio, Universidad de Ejemplo, 2024-02 a 2024-11.",
        "Educacion: Ingenieria en Ciencia de Datos, Universidad de Ejemplo, 2021-01.",
        "Habilidades: Python, SQL, Power BI, trabajo en equipo.",
    };

    private DocumentosDePrueba() {}

    public static byte[] pdf(String... lineas) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            try (PDPageContentStream contenido = new PDPageContentStream(doc, pagina)) {
                contenido.beginText();
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                contenido.newLineAtOffset(50, 700);
                for (String linea : lineas) {
                    contenido.showText(linea);
                    contenido.newLineAtOffset(0, -16);
                }
                contenido.endText();
            }
            doc.save(salida);
            return salida.toByteArray();
        }
    }

    public static byte[] docx(String... lineas) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            for (String linea : lineas) {
                doc.createParagraph().createRun().setText(linea);
            }
            doc.write(salida);
            return salida.toByteArray();
        }
    }
}
