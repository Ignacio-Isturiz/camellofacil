package com.camellofacil.backend.cv.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.camellofacil.backend.cv.app.ModelosAnalisis.Observacion;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorEducacion;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorExperiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Las reglas del análisis: explicables y siempre con el mismo resultado para la misma hoja. */
class EvaluadorHojaTest {

    private static final ValoresHoja COMPLETA = new ValoresHoja(
            "Ana Prueba", "300 000 0000", "Medellín",
            "Estudiante de ingeniería enfocada en análisis de datos. Busco una práctica donde aplicarlo.",
            "Python, SQL, Excel, Power BI, Git, Docker, Linux, Inglés B2",
            List.of(
                    new ValorExperiencia("e1", "Practicante de datos", "Empresa Ejemplo", "2025-06", "",
                            "Construí dashboards con Power BI que redujeron 30% el tiempo de reporte semanal para el área comercial."),
                    new ValorExperiencia("e2", "Monitora", "Universidad de Ejemplo", "2024-02", "2024-11",
                            "Apoyé las prácticas de laboratorio de 40 estudiantes y preparé los materiales de cada sesión.")),
            List.of(new ValorEducacion("d1", "Ingeniería de Sistemas", "Universidad de Ejemplo", "2021-01", "")));

    private static final ValoresHoja VACIA = new ValoresHoja("", "", "", "", "", List.of(), List.of());

    private static Map<String, Observacion> porId(ValoresHoja v) {
        return EvaluadorHoja.observaciones(v).stream().collect(Collectors.toMap(Observacion::id, Function.identity()));
    }

    @Test
    void unaHojaCompletaYOrdenadaSacaPuntajeAltoEnHarvardYEnAts() {
        assertThat(EvaluadorHoja.harvard(COMPLETA).valor()).isGreaterThanOrEqualTo(90);
        assertThat(EvaluadorHoja.harvard(COMPLETA).nivel()).isEqualTo("alto");
        assertThat(EvaluadorHoja.ats(COMPLETA, "general").orElseThrow().valor()).isGreaterThanOrEqualTo(90);
        assertThat(EvaluadorHoja.observaciones(COMPLETA)).extracting(Observacion::tipo).containsOnly("bien");
    }

    @Test
    void unaHojaVaciaSacaPuntajeBajoYTodoEsFaltaOMejorar() {
        assertThat(EvaluadorHoja.harvard(VACIA).nivel()).isEqualTo("bajo");
        assertThat(EvaluadorHoja.harvard(VACIA).valor()).isLessThan(30);
        assertThat(EvaluadorHoja.ats(VACIA, "general").orElseThrow().nivel()).isEqualTo("bajo");
        assertThat(porId(VACIA).get("h-perfil").tipo()).isEqualTo("falta");
        assertThat(porId(VACIA).get("h-experiencia").tipo()).isEqualTo("falta");
    }

    @Test
    void elMismoContenidoDaSiempreElMismoResultado() {
        assertThat(EvaluadorHoja.harvard(COMPLETA)).isEqualTo(EvaluadorHoja.harvard(COMPLETA));
        assertThat(EvaluadorHoja.observaciones(COMPLETA)).isEqualTo(EvaluadorHoja.observaciones(COMPLETA));
    }

    @Test
    void elOrdenCronologicoSeDetectaEnExperienciaYEstudios() {
        ValoresHoja alReves = new ValoresHoja(COMPLETA.nombre(), COMPLETA.telefono(), COMPLETA.ciudad(), COMPLETA.perfil(), COMPLETA.habilidades(),
                List.of(COMPLETA.experiencia().get(1), COMPLETA.experiencia().get(0)), COMPLETA.educacion());
        assertThat(porId(COMPLETA).get("h-orden").tipo()).isEqualTo("bien");
        assertThat(porId(alReves).get("h-orden").tipo()).isEqualTo("mejorar");
        assertThat(EvaluadorHoja.harvard(alReves).valor()).isLessThan(EvaluadorHoja.harvard(COMPLETA).valor());
    }

    @Test
    void laRedaccionSeEvaluaPorDescripcionYCifras() {
        ValoresHoja sinCifras = new ValoresHoja(COMPLETA.nombre(), COMPLETA.telefono(), COMPLETA.ciudad(), COMPLETA.perfil(), COMPLETA.habilidades(),
                List.of(new ValorExperiencia("e1", "Practicante", "Empresa", "2025-06", "", "Ayudé con reportes y atención de solicitudes del área.")),
                COMPLETA.educacion());
        ValoresHoja sinDescripcion = new ValoresHoja(COMPLETA.nombre(), COMPLETA.telefono(), COMPLETA.ciudad(), COMPLETA.perfil(), COMPLETA.habilidades(),
                List.of(new ValorExperiencia("e1", "Practicante", "Empresa", "2025-06", "", "")), COMPLETA.educacion());

        assertThat(porId(sinCifras).get("h-logros").tipo()).isEqualTo("mejorar");
        assertThat(porId(sinCifras).get("h-logros").detalle()).contains("cifras");
        assertThat(porId(sinDescripcion).get("h-logros").tipo()).isEqualTo("falta");
        assertThat(porId(sinDescripcion).get("a-descripciones").tipo()).isEqualTo("falta");
    }

    @Test
    void lasFechasSinAnoYMesYElTextoRaroBajanElAts() {
        ValoresHoja mal = new ValoresHoja(COMPLETA.nombre(), COMPLETA.telefono(), COMPLETA.ciudad(), COMPLETA.perfil() + " 🚀", COMPLETA.habilidades(),
                List.of(new ValorExperiencia("e1", "Practicante de datos", "Empresa Ejemplo", "2025", "", COMPLETA.experiencia().get(0).descripcion())),
                COMPLETA.educacion());

        assertThat(porId(mal).get("a-fechas").tipo()).isEqualTo("mejorar");
        assertThat(porId(mal).get("a-texto").tipo()).isEqualTo("mejorar");
        assertThat(EvaluadorHoja.ats(mal, "general").orElseThrow().valor()).isLessThan(EvaluadorHoja.ats(COMPLETA, "general").orElseThrow().valor());
    }

    @Test
    void lasOpcionesAtsPesanDistintoYUnaOpcionInexistenteNoExiste() {
        // Pocas habilidades: la opción de palabras clave castiga más que la de estructura.
        ValoresHoja pocas = new ValoresHoja(COMPLETA.nombre(), COMPLETA.telefono(), COMPLETA.ciudad(), COMPLETA.perfil(), "Excel",
                COMPLETA.experiencia(), COMPLETA.educacion());

        int claves = EvaluadorHoja.ats(pocas, "palabras-clave").orElseThrow().valor();
        int estructura = EvaluadorHoja.ats(pocas, "estructura").orElseThrow().valor();
        assertThat(claves).isLessThan(estructura);
        assertThat(EvaluadorHoja.ats(pocas, "palabras-clave").orElseThrow().opcionId()).isEqualTo("palabras-clave");
        assertThat(EvaluadorHoja.ats(pocas, "no-existe")).isEmpty();
        assertThat(EvaluadorHoja.OPCIONES_ATS).extracting("id").containsExactly("general", "palabras-clave", "estructura");
    }

    @Test
    void loQueMasPesaMueveMasElPuntaje() {
        ValoresHoja sinExperiencia = new ValoresHoja(COMPLETA.nombre(), COMPLETA.telefono(), COMPLETA.ciudad(), COMPLETA.perfil(), COMPLETA.habilidades(),
                List.of(), COMPLETA.educacion());
        ValoresHoja sinTelefono = new ValoresHoja(COMPLETA.nombre(), "", COMPLETA.ciudad(), COMPLETA.perfil(), COMPLETA.habilidades(),
                COMPLETA.experiencia(), COMPLETA.educacion());

        assertThat(EvaluadorHoja.harvard(sinExperiencia).valor()).isLessThan(EvaluadorHoja.harvard(sinTelefono).valor());
    }

    @Test
    void elPuntajeSiempreQuedaEntre0Y100() {
        for (ValoresHoja v : List.of(COMPLETA, VACIA)) {
            assertThat(EvaluadorHoja.harvard(v).valor()).isBetween(0, 100);
            for (var o : EvaluadorHoja.OPCIONES_ATS) assertThat(EvaluadorHoja.ats(v, o.id()).orElseThrow().valor()).isBetween(0, 100);
        }
    }
}
