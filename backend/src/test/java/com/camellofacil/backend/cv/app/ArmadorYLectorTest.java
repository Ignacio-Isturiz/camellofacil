package com.camellofacil.backend.cv.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.camellofacil.backend.cv.DocumentosDePrueba;
import com.camellofacil.backend.cv.app.ExtractorIa.EducacionIa;
import com.camellofacil.backend.cv.app.ExtractorIa.ExperienciaIa;
import com.camellofacil.backend.cv.app.ExtractorIa.Extraccion;
import com.camellofacil.backend.cv.app.ModelosHoja.DatosHoja;
import com.camellofacil.backend.cv.app.ModelosHoja.TipoArchivo;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArmadorYLectorTest {

    private final LectorDocumentos lector = new LectorDocumentos();

    @Test
    void reconoceElTipoPorElContenidoYNoPorLaExtension() throws Exception {
        assertThat(lector.detectarTipo(DocumentosDePrueba.pdf("hola"))).contains(TipoArchivo.PDF);
        assertThat(lector.detectarTipo(DocumentosDePrueba.docx("hola"))).contains(TipoArchivo.DOCX);
        assertThat(lector.detectarTipo("solo texto plano".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(lector.detectarTipo(new byte[] {(byte) 0x89, 'P', 'N', 'G'})).isEmpty();
    }

    @Test
    void leeElTextoDePdfYWord() throws Exception {
        String pdf = lector.leer(DocumentosDePrueba.pdf(DocumentosDePrueba.LINEAS), TipoArchivo.PDF).texto();
        String docx = lector.leer(DocumentosDePrueba.docx(DocumentosDePrueba.LINEAS), TipoArchivo.DOCX).texto();
        assertThat(pdf).contains("Ana Prueba Ficticia").contains("Python, SQL");
        assertThat(docx).contains("Ana Prueba Ficticia").contains("Python, SQL");
    }

    @Test
    void unArchivoCasiSinTextoEsIlegible() throws Exception {
        byte[] casiVacio = DocumentosDePrueba.pdf("Hoja");
        assertThatThrownBy(() -> lector.leer(casiVacio, TipoArchivo.PDF))
                .isInstanceOf(LectorDocumentos.ArchivoIlegibleException.class);
        byte[] falsoDocx = {'P', 'K', 3, 4, 0, 0, 0};
        assertThatThrownBy(() -> lector.leer(falsoDocx, TipoArchivo.DOCX))
                .isInstanceOf(LectorDocumentos.ArchivoIlegibleException.class);
    }

    @Test
    void loQueLaIaInventaQuedaParaRevisar() {
        String texto = String.join("\n", DocumentosDePrueba.LINEAS);
        Extraccion ia = new Extraccion(
                "es",
                "ANA PRUEBA FICTICIA", // mismo nombre con otras mayúsculas: se acepta
                "300 123 4567",
                "Bogotá", // no está en el archivo
                "Perfil resumido",
                "Python, SQL",
                List.of(
                        new ExperienciaIa("Monitora de laboratorio", "Universidad de Ejemplo", "2024-02", "2024-11", "Apoyo"),
                        new ExperienciaIa("Gerente general", "Empresa Inventada", "2020", null, null)),
                List.of(new EducacionIa("Ingeniería en Ciencia de Datos", "Universidad de Ejemplo", "2021-01", null)));

        DatosHoja d = ArmadorDatos.armar(ia, texto);

        assertThat(d.contacto().nombre().estado()).isEqualTo(ModelosHoja.LEIDO);
        assertThat(d.contacto().telefono().estado()).isEqualTo(ModelosHoja.LEIDO);
        assertThat(d.contacto().ciudad().estado()).isEqualTo(ModelosHoja.REVISA);
        assertThat(d.experiencia().get(0).cargo().estado()).isEqualTo(ModelosHoja.LEIDO);
        assertThat(d.experiencia().get(1).cargo().estado()).isEqualTo(ModelosHoja.REVISA);
        assertThat(d.experiencia().get(1).organizacion().estado()).isEqualTo(ModelosHoja.REVISA);
        // «2020» no tiene el formato AAAA-MM: se pide revisarlo y no se guarda un valor dudoso.
        assertThat(d.experiencia().get(1).inicio().estado()).isEqualTo(ModelosHoja.REVISA);
        assertThat(d.experiencia().get(1).fin().estado()).isEqualTo(ModelosHoja.NO_ENCONTRADO);
        // «Ingeniería» con tilde coincide con «Ingenieria» sin tilde del archivo.
        assertThat(d.educacion().get(0).titulo().estado()).isEqualTo(ModelosHoja.LEIDO);
        assertThat(ArmadorDatos.pendientes(d)).isEqualTo(4);
    }

    @Test
    void recortaRespuestasAnomalas() {
        List<ExperienciaIa> muchas = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> new ExperienciaIa("x".repeat(500), null, null, null, null))
                .toList();
        DatosHoja d = ArmadorDatos.armar(new Extraccion("xx", null, null, null, null, null, muchas, null), "texto");
        assertThat(d.experiencia()).hasSize(ArmadorDatos.MAXIMO_ITEMS);
        assertThat(d.experiencia().get(0).cargo().valor()).hasSize(150);
        assertThat(d.educacion()).isEmpty();
        assertThat(ArmadorDatos.idioma(new Extraccion("xx", null, null, null, null, null, null, null))).isEqualTo("es");
    }
}
