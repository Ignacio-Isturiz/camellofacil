package com.camellofacil.backend.adaptaciones.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.ExperienciaIa;
import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.PropuestaIa;
import com.camellofacil.backend.adaptaciones.app.VerificadorFidelidad.Resultado;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorEducacion;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorExperiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import java.util.List;
import org.junit.jupiter.api.Test;

/** RN-03: la adaptación solo puede reordenar y reescribir con lo que ya está en la hoja base. */
class VerificadorFidelidadTest {

    private static final ValoresHoja BASE = new ValoresHoja(
            "Ana Prueba", "300 000 0000", "Medellín",
            "Estudiante de ingeniería con experiencia en análisis de datos con Python.",
            "Python, SQL, Excel",
            List.of(
                    new ValorExperiencia("e1", "Monitora", "Universidad de Ejemplo", "2024-02", "2024-11",
                            "Apoyé prácticas de laboratorio y atendí a 30 estudiantes."),
                    new ValorExperiencia("e2", "Auxiliar", "Tienda Ejemplo", "2023-01", "2023-12", "Atención al cliente y manejo de caja.")),
            List.of(new ValorEducacion("d1", "Ingeniería de Sistemas", "Universidad de Ejemplo", "2021-01", "")));

    private static PropuestaIa propuesta(String perfil, List<String> habilidades, List<ExperienciaIa> exp, List<String> avisos) {
        return new PropuestaIa(perfil, habilidades, exp, avisos);
    }

    @Test
    void reordenaExperienciasYHabilidadesQueYaEstaban() {
        Resultado r = VerificadorFidelidad.verificar(BASE, propuesta(null, List.of("SQL", "Python"),
                List.of(new ExperienciaIa("e2", null), new ExperienciaIa("e1", null)), List.of()));

        assertThat(r.valores().experiencia()).extracting(ValorExperiencia::id).containsExactly("e2", "e1");
        assertThat(r.valores().habilidades()).isEqualTo("SQL, Python, Excel");
        assertThat(r.cambios()).extracting("tipo").containsExactly("movido", "destacado");
    }

    @Test
    void nuncaAgregaHabilidadesNiExperienciasQueNoEstanEnLaHoja() {
        Resultado r = VerificadorFidelidad.verificar(BASE, propuesta(null, List.of("Kubernetes", "SQL"),
                List.of(new ExperienciaIa("inventada", "Dirigí un equipo de 10 personas"), new ExperienciaIa("e1", null)), List.of()));

        assertThat(r.valores().habilidades()).doesNotContain("Kubernetes").isEqualTo("SQL, Python, Excel");
        assertThat(r.valores().experiencia()).extracting(ValorExperiencia::id).containsExactly("e1", "e2");
    }

    @Test
    void aceptaUnaReescrituraFielYDescartaLaQueInventaCifrasHerramientasOLugares() {
        Resultado fiel = VerificadorFidelidad.verificar(BASE, propuesta(null, List.of(),
                List.of(new ExperienciaIa("e1", "Brindé apoyo en prácticas de laboratorio y atendí a 30 estudiantes.")), List.of()));
        assertThat(fiel.valores().experiencia().get(0).descripcion()).startsWith("Brindé apoyo");
        assertThat(fiel.cambios()).extracting("tipo").contains("redaccion");

        for (String inventada : List.of(
                "Atendí a 300 estudiantes.",                 // cifra que no está
                "Analicé datos con Power BI.",               // herramienta que no está
                "Atendí estudiantes en Bogotá.",             // lugar que no está
                "Atendí estudiantes con KPIS.")) {           // sigla que no está
            Resultado r = VerificadorFidelidad.verificar(BASE, propuesta(null, List.of(),
                    List.of(new ExperienciaIa("e1", inventada)), List.of()));
            assertThat(r.valores().experiencia().get(0).descripcion()).as(inventada).isEqualTo(BASE.experiencia().get(0).descripcion());
            assertThat(r.cambios()).as(inventada).isEmpty();
        }
    }

    @Test
    void elPerfilSoloSeReescribeSiYaExistiaYEsFiel() {
        Resultado fiel = VerificadorFidelidad.verificar(BASE, propuesta(
                "Estudiante de ingeniería enfocada en análisis de datos con Python.", List.of(), List.of(), List.of()));
        assertThat(fiel.valores().perfil()).startsWith("Estudiante de ingeniería enfocada");

        Resultado inventado = VerificadorFidelidad.verificar(BASE, propuesta(
                "Ingeniera con 5 años de experiencia liderando proyectos.", List.of(), List.of(), List.of()));
        assertThat(inventado.valores().perfil()).isEqualTo(BASE.perfil());

        ValoresHoja sinPerfil = new ValoresHoja(BASE.nombre(), BASE.telefono(), BASE.ciudad(), "", BASE.habilidades(), BASE.experiencia(), BASE.educacion());
        Resultado creado = VerificadorFidelidad.verificar(sinPerfil, propuesta("Perfil nuevo que la IA escribió.", List.of(), List.of(), List.of()));
        assertThat(creado.valores().perfil()).isEmpty();
    }

    @Test
    void cargosOrganizacionesFechasYEstudiosSiempreSonLosDeLaHoja() {
        Resultado r = VerificadorFidelidad.verificar(BASE, propuesta("Estudiante de ingeniería.", List.of("SQL"),
                List.of(new ExperienciaIa("e2", "Atención al cliente y manejo de caja."), new ExperienciaIa("e1", null)), List.of()));

        ValorExperiencia e1 = r.valores().experiencia().get(1);
        assertThat(e1.cargo()).isEqualTo("Monitora");
        assertThat(e1.organizacion()).isEqualTo("Universidad de Ejemplo");
        assertThat(e1.inicio()).isEqualTo("2024-02");
        assertThat(r.valores().educacion()).isEqualTo(BASE.educacion());
        assertThat(r.valores().nombre()).isEqualTo("Ana Prueba");
    }

    @Test
    void losAvisosSeLimpianYSeLimitan() {
        Resultado r = VerificadorFidelidad.verificar(BASE, propuesta(null, List.of(), List.of(),
                List.of("a", " ", "b", "c", "d", "e", "f", "g".repeat(500))));
        assertThat(r.avisos()).hasSize(5).doesNotContain(" ");
        assertThat(r.avisos()).allMatch(a -> a.length() <= 200);
    }
}
