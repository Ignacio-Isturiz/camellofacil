package com.camellofacil.backend.vacantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camellofacil.backend.FuenteDePrueba;
import com.camellofacil.backend.PruebaIntegracion;
import com.camellofacil.backend.auth.app.UsuarioActual;
import com.camellofacil.backend.auth.app.UsuarioService;
import com.camellofacil.backend.perfil.app.ModelosPerfil.DatosBasicos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.TipoPersona;
import com.camellofacil.backend.perfil.app.PerfilService;
import com.camellofacil.backend.vacantes.app.IngestaVacantesService;
import com.camellofacil.backend.vacantes.app.IngestaVacantesService.Resultado;
import com.camellofacil.backend.vacantes.app.ModelosVacante.VacanteExterna;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Ingesta con cupo y prioridad (fuente falsa), duplicados, cierre de vencidas y la API de vacantes. */
class VacantesIntegracionTest extends PruebaIntegracion {

    @Autowired
    IngestaVacantesService ingesta;

    @Autowired
    UsuarioService usuarios;

    @Autowired
    PerfilService perfiles;

    @Autowired
    JdbcTemplate jdbc;

    private RequestPostProcessor persona;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM vacante");
        jdbc.update("DELETE FROM uso_fuente");
        jdbc.update("DELETE FROM consulta_vacantes WHERE fuente = ?", FuenteDePrueba.CODIGO);
        insertarConsulta("informatica", "practicante sistemas", 5);
        insertarConsulta("derecho", "practicante derecho", 1);
        FuenteDePrueba.BUSQUEDAS.clear();
        FuenteDePrueba.SOLO_REMOTAS.set(false);
        FuenteDePrueba.ADELANTO.set(1);
        FuenteDePrueba.RESPUESTA.set(List.of(
                oferta("1", "Practicante de Ingeniería de Sistemas", "Empresa Ficticia", "Medellín", "https://portal.example/1"),
                // La misma oferta en otro portal (otras mayúsculas y sin tilde): debe quedar una sola
                oferta("2", "PRACTICANTE DE INGENIERIA DE SISTEMAS", "EMPRESA FICTICIA", "Medellin", "https://otro.example/2"),
                oferta("3", "Analista de datos senior", "Otra Empresa", "Envigado, Antioquia", "https://portal.example/3"),
                oferta("4", "Practicante", "Empresa Ficticia", null, "javascript:alert(1)"),
                // Presencial fuera del área metropolitana de Medellín: no sirve
                oferta("5", "Practicante de sistemas", "Empresa Lejana", "Bogotá", "https://portal.example/5")));

        String oid = UUID.randomUUID().toString();
        UsuarioActual u = usuarios.registrarAcceso(UsuarioService.PROVEEDOR_MICROSOFT, oid, "vac." + oid + "@unaula.edu.co", "Persona Prueba");
        perfiles.completar(u.id(), new DatosBasicos(TipoPersona.ESTUDIANTE, "informatica", 6));
        persona = oidcLogin().idToken(t -> t.claim("oid", oid));
    }

    private void insertarConsulta(String carrera, String texto, int peso) {
        jdbc.update("INSERT INTO consulta_vacantes (id, fuente, carrera_codigo, texto, peso) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), FuenteDePrueba.CODIGO, carrera, texto, peso);
    }

    private static VacanteExterna oferta(String id, String cargo, String empresa, String ciudad, String url) {
        return new VacanteExterna(id, cargo, empresa, ciudad, false, "", "Portal Ficticio", url,
                "Descripción inventada para pruebas.", List.of("Soporte"), List.of("Estudiante"),
                Instant.now().minus(Duration.ofDays(2)));
    }

    private Resultado rondaDePrueba() {
        return ingesta.ronda().stream().filter(r -> r.fuente().equals(FuenteDePrueba.CODIGO)).findFirst().orElseThrow();
    }

    @Test
    void unaRondaTraeGuardaSinDuplicadosYRespetaElCupo() {
        Resultado primera = rondaDePrueba();
        assertThat(primera.consulto()).isTrue();
        assertThat(primera.recibidas()).isEqualTo(5);
        assertThat(primera.guardadas()).isEqualTo(3); // se descartan el enlace javascript: y la de Bogotá
        // Carga inicial: cada búsqueda corre una vez, empezando por la de mayor prioridad
        assertThat(FuenteDePrueba.BUSQUEDAS).containsExactly("practicante sistemas", "practicante derecho");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vacante", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT consumidas FROM uso_fuente WHERE fuente = 'prueba'", Integer.class)).isEqualTo(2);

        // Después, el cupo parejo (10 por día) no permite más por ahora: la segunda ronda no consulta
        assertThat(rondaDePrueba().consulto()).isFalse();
        assertThat(FuenteDePrueba.BUSQUEDAS).hasSize(2);
    }

    @Test
    void conAdelantoUnaRondaHaceVariasConsultasSinRepetirNiPasarse() {
        FuenteDePrueba.ADELANTO.set(3);
        List<Resultado> ronda = ingesta.ronda().stream().filter(r -> r.fuente().equals(FuenteDePrueba.CODIGO)).toList();
        // Solo hay 2 búsquedas; la tercera petición repite la de mayor peso. Luego ya no hay cupo.
        assertThat(ronda).extracting(Resultado::consulto).containsExactly(true, true, true, false);
        assertThat(FuenteDePrueba.BUSQUEDAS).containsExactly("practicante sistemas", "practicante derecho", "practicante sistemas");
        assertThat(jdbc.queryForObject("SELECT consumidas FROM uso_fuente WHERE fuente = 'prueba'", Integer.class)).isEqualTo(3);
    }

    @Test
    void siLaFuenteFallaNoSigueConsultandoEnLaMismaRonda() {
        FuenteDePrueba.ADELANTO.set(5);
        FuenteDePrueba.RESPUESTA.set(null);
        ingesta.ronda();
        assertThat(FuenteDePrueba.BUSQUEDAS).hasSize(1);
    }

    @Test
    void deUnaFuenteInternacionalSoloSeGuardanLasRemotas() {
        FuenteDePrueba.SOLO_REMOTAS.set(true);
        FuenteDePrueba.RESPUESTA.set(List.of(
                new VacanteExterna("r1", "Software Engineering Intern", "Empresa Remota", "Remote", true, "Internship",
                        "portal.example", "https://portal.example/r1", "Fully remote.", List.of(), List.of(), null),
                new VacanteExterna("r2", "Software Engineer Intern", "Empresa Presencial", "Torrance, CA", false, "",
                        "portal.example", "https://portal.example/r2", "On-site.", List.of(), List.of(), null)));

        Resultado r = rondaDePrueba();

        assertThat(r.guardadas()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT empresa FROM vacante", String.class)).isEqualTo("Empresa Remota");
        assertThat(jdbc.queryForObject("SELECT modalidad || ' ' || tipo FROM vacante", String.class)).isEqualTo("REMOTO PRACTICA");
    }

    @Test
    void laBusquedaSiguienteEsLaQueMasLeTocaSegunSuPeso() {
        // La de sistemas (peso 5) se ejecutó hace 1 hora; la de derecho (peso 1) hace 4 horas: 1×5 > 4×1
        jdbc.update("UPDATE consulta_vacantes SET ultima_ejecucion = now() - interval '1 hour' WHERE texto = 'practicante sistemas' AND fuente = 'prueba'");
        jdbc.update("UPDATE consulta_vacantes SET ultima_ejecucion = now() - interval '4 hours' WHERE texto = 'practicante derecho' AND fuente = 'prueba'");
        sinCargaInicialYConCupoParaUna();
        rondaDePrueba();
        assertThat(FuenteDePrueba.BUSQUEDAS).containsExactly("practicante sistemas");

        // Con 6 horas sin ejecutarse, derecho ya supera a sistemas (6×1 > 1×5)
        jdbc.update("UPDATE consulta_vacantes SET ultima_ejecucion = now() - interval '1 hour' WHERE texto = 'practicante sistemas' AND fuente = 'prueba'");
        jdbc.update("UPDATE consulta_vacantes SET ultima_ejecucion = now() - interval '6 hours' WHERE texto = 'practicante derecho' AND fuente = 'prueba'");
        sinCargaInicialYConCupoParaUna();
        rondaDePrueba();
        assertThat(FuenteDePrueba.BUSQUEDAS).containsExactly("practicante sistemas", "practicante derecho");
    }

    /** Periodo empezado hace 4 horas con la carga inicial (2) ya gastada: el cupo parejo permite justo una más. */
    private void sinCargaInicialYConCupoParaUna() {
        jdbc.update("DELETE FROM uso_fuente");
        jdbc.update("INSERT INTO uso_fuente (fuente, periodo_inicio, consumidas) VALUES ('prueba', now() - interval '4 hours', 2)");
    }

    @Test
    void siLaLlaveEsRechazadaLaFuenteSePausaUnDia() {
        FuenteDePrueba.RESPUESTA.set(null);
        rondaDePrueba();
        assertThat(jdbc.queryForObject(
                "SELECT pausada_hasta > now() + interval '23 hours' FROM uso_fuente WHERE fuente = 'prueba'", Boolean.class))
                .isTrue();
        // Aunque el cupo lo permitiera, mientras está pausada no consulta
        jdbc.update("UPDATE uso_fuente SET consumidas = 0, periodo_inicio = now() - interval '12 hours'");
        assertThat(rondaDePrueba().consulto()).isFalse();
    }

    @Test
    void lasVacantesQueYaNoAparecenSeCierran() {
        rondaDePrueba();
        jdbc.update("UPDATE vacante SET vista_en = now() - interval '22 days' WHERE cargo LIKE 'Analista%'");
        rondaDePrueba(); // sin cupo para consultar, pero igual cierra las vencidas
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vacante WHERE cerrada_en IS NULL", Integer.class)).isEqualTo(1);
    }

    @Test
    void laApiListaMuestraElDetalleYGuardaParaLaPersona() throws Exception {
        rondaDePrueba();
        String practica = jdbc.queryForObject("SELECT id FROM vacante WHERE tipo = 'PRACTICA'", UUID.class).toString();

        mockMvc.perform(get("/api/v1/vacantes").with(persona))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vacantes.length()").value(2))
                .andExpect(jsonPath("$.guardadas.length()").value(0));

        mockMvc.perform(get("/api/v1/vacantes/" + practica).with(persona))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vacante.tipo").value("practica"))
                .andExpect(jsonPath("$.vacante.modalidad").value("presencial"))
                .andExpect(jsonPath("$.vacante.fuente").value("Portal Ficticio"))
                .andExpect(jsonPath("$.vacante.carreras").value(org.hamcrest.Matchers.hasItem("informatica")))
                .andExpect(jsonPath("$.requisitos[0]").value("Estudiante"))
                // El enlace original no se expone aquí: se entrega al postularse (y queda registrado)
                .andExpect(jsonPath("$.vacante.url").doesNotExist());

        mockMvc.perform(put("/api/v1/vacantes/" + practica + "/guardada").with(persona).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/vacantes").with(persona))
                .andExpect(jsonPath("$.guardadas[0]").value(practica));
        mockMvc.perform(delete("/api/v1/vacantes/" + practica + "/guardada").with(persona).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/vacantes").with(persona))
                .andExpect(jsonPath("$.guardadas.length()").value(0));

        mockMvc.perform(put("/api/v1/vacantes/" + UUID.randomUUID() + "/guardada").with(persona).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("vacante_no_encontrada"));
    }

    @Test
    void sinSesionNoHayVacantes() throws Exception {
        mockMvc.perform(get("/api/v1/vacantes")).andExpect(status().isUnauthorized());
    }
}
