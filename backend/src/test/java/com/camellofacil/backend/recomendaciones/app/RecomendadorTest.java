package com.camellofacil.backend.recomendaciones.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.camellofacil.backend.perfil.app.ModelosPerfil.CarreraDatos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.PerfilDatos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.Preferencias;
import com.camellofacil.backend.perfil.app.ModelosPerfil.TipoPersona;
import com.camellofacil.backend.recomendaciones.app.Recomendador.Razon;
import com.camellofacil.backend.recomendaciones.app.Recomendador.Recomendacion;
import com.camellofacil.backend.vacantes.app.ModelosVacante.Modalidad;
import com.camellofacil.backend.vacantes.app.ModelosVacante.TipoVacante;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteResumen;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecomendadorTest {

    private static final Instant AHORA = Instant.parse("2026-11-17T12:00:00Z");
    private static final CarreraDatos INFORMATICA = new CarreraDatos("informatica", "Ingeniería Informática");

    private static VacanteResumen vacante(String cargo, TipoVacante tipo, Modalidad modalidad, int dias, String... carreras) {
        return new VacanteResumen(UUID.randomUUID(), cargo, "Empresa Ficticia", tipo, "Medellín", modalidad,
                AHORA.minus(Duration.ofDays(dias)), "Portal", List.of(carreras));
    }

    private static List<String> cargos(List<Recomendacion> r) {
        return r.stream().map(x -> x.vacante().cargo()).toList();
    }

    @Test
    void soloRecomiendaLasDeSuCarreraYPrimeroPracticasSiEstudia() {
        PerfilDatos estudiante = new PerfilDatos(TipoPersona.ESTUDIANTE, INFORMATICA, 6, null);
        List<VacanteResumen> abiertas = List.of(
                vacante("Desarrollador junior", TipoVacante.PROFESIONAL, Modalidad.PRESENCIAL, 0, "informatica"),
                vacante("Practicante de sistemas", TipoVacante.PRACTICA, Modalidad.PRESENCIAL, 5, "informatica"),
                vacante("Practicante contable", TipoVacante.PRACTICA, Modalidad.REMOTO, 0, "contaduria"),
                vacante("Practicante de datos", TipoVacante.PRACTICA, Modalidad.HIBRIDO, 1, "datos", "informatica"));

        var r = Recomendador.recomendar(estudiante, abiertas, Set.of(), AHORA, 6);

        assertThat(cargos(r.recomendadas()))
                .containsExactly("Practicante de datos", "Practicante de sistemas", "Desarrollador junior");
        assertThat(r.recomendadas().get(0).razones()).containsExactly(Razon.CARRERA, Razon.TIPO);
        assertThat(r.recomendadas().get(0).nueva()).isTrue();
        assertThat(r.recomendadas().get(1).nueva()).isFalse();
        assertThat(r.nuevasDeLaSemana()).isEqualTo(3);
    }

    @Test
    void siEgresoPrimeroEmpleosYLuegoLaModalidadQuePrefiere() {
        PerfilDatos egresado = new PerfilDatos(TipoPersona.EGRESADO, INFORMATICA, null,
                new Preferencias(Set.of(com.camellofacil.backend.perfil.app.ModelosPerfil.Modalidad.REMOTO), null));
        List<VacanteResumen> abiertas = List.of(
                vacante("Practicante remoto", TipoVacante.PRACTICA, Modalidad.REMOTO, 0, "informatica"),
                vacante("Desarrollador presencial", TipoVacante.PROFESIONAL, Modalidad.PRESENCIAL, 0, "informatica"),
                vacante("Desarrollador remoto", TipoVacante.PROFESIONAL, Modalidad.REMOTO, 3, "informatica"));

        var r = Recomendador.recomendar(egresado, abiertas, Set.of(), AHORA, 2);

        assertThat(cargos(r.recomendadas())).containsExactly("Desarrollador remoto", "Desarrollador presencial");
        assertThat(r.recomendadas().get(0).razones()).containsExactly(Razon.CARRERA, Razon.TIPO, Razon.MODALIDAD);
    }

    @Test
    void sinPerfilOConTodoExcluidoNoRecomiendaNada() {
        VacanteResumen v = vacante("Practicante", TipoVacante.PRACTICA, Modalidad.REMOTO, 0, "informatica");
        PerfilDatos estudiante = new PerfilDatos(TipoPersona.ESTUDIANTE, INFORMATICA, 6, null);

        assertThat(Recomendador.recomendar(null, List.of(v), Set.of(), AHORA, 6).recomendadas()).isEmpty();
        var excluida = Recomendador.recomendar(estudiante, List.of(v), Set.of(v.id()), AHORA, 6);
        assertThat(excluida.recomendadas()).isEmpty();
        assertThat(excluida.nuevasDeLaSemana()).isZero();
    }
}
