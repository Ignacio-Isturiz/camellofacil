package com.camellofacil.backend.postulaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camellofacil.backend.PruebaIntegracion;
import com.camellofacil.backend.auth.app.UsuarioActual;
import com.camellofacil.backend.auth.app.UsuarioService;
import com.camellofacil.backend.perfil.app.ModelosPerfil.DatosBasicos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.TipoPersona;
import com.camellofacil.backend.perfil.app.PerfilService;
import com.camellofacil.backend.postulaciones.app.PostulacionesService;
import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** «Postular» devuelve la página original y deja el historial; cada persona solo ve y toca el suyo. */
class PostulacionesIntegracionTest extends PruebaIntegracion {

    @Autowired
    UsuarioService usuarios;

    @Autowired
    PerfilService perfiles;

    @Autowired
    PostulacionesService postulaciones;

    @Autowired
    JdbcTemplate jdbc;

    private RequestPostProcessor persona;
    private RequestPostProcessor otra;
    private UUID practica;
    private UUID remota;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM vacante");
        practica = vacante("Practicante de Ingeniería Informática", "Empresa Ficticia", "Medellín", "PRESENCIAL",
                "https://empresa.example/ofertas/1");
        remota = vacante("Analista de datos", "Banco de Ejemplo", null, "REMOTO", "https://banco.example/empleo/2");
        persona = nuevaPersona();
        otra = nuevaPersona();
    }

    private RequestPostProcessor nuevaPersona() {
        String oid = UUID.randomUUID().toString();
        UsuarioActual u = usuarios.registrarAcceso(
                UsuarioService.PROVEEDOR_MICROSOFT, oid, "pos." + oid + "@unaula.edu.co", "Persona Prueba");
        perfiles.completar(u.id(), new DatosBasicos(TipoPersona.ESTUDIANTE, "informatica", 6));
        return oidcLogin().idToken(t -> t.claim("oid", oid));
    }

    private UUID vacante(String cargo, String empresa, String ciudad, String modalidad, String url) {
        UUID id = UUID.randomUUID();
        Timestamp ahora = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO vacante (id, fuente, id_externo, huella, cargo, empresa, ciudad, modalidad, tipo,
                                     url_postulacion, publicada_en, vista_en, creada_en)
                VALUES (?, 'prueba', ?, ?, ?, ?, ?, ?, 'PRACTICA', ?, ?, ?, ?)""",
                id, id.toString(), id.toString().replace("-", "") + "0".repeat(32), cargo, empresa, ciudad, modalidad,
                url, ahora, ahora, ahora);
        return id;
    }

    private String postular(UUID vacanteId, String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/v1/vacantes/" + vacanteId + "/clics").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void postularDevuelveLaPaginaOriginalYQuedaEnElHistorial() throws Exception {
        String r = postular(practica, "{\"hoja\":\"base\"}");
        assertThat(JsonPath.<String>read(r, "$.urlOriginal")).isEqualTo("https://empresa.example/ofertas/1");

        // Segundo clic con la hoja adaptada: la misma fila, dos clics y la última hoja
        postular(practica, "{\"hoja\":\"adaptada\"}");
        postular(remota, "{}");

        mockMvc.perform(get("/api/v1/postulaciones").with(persona))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.clicsUltimos90Dias").value(3))
                .andExpect(jsonPath("$.items[0].cargo").value("Analista de datos"))
                .andExpect(jsonPath("$.items[0].modalidad").value("remoto"))
                .andExpect(jsonPath("$.items[0].ciudad").doesNotExist())
                .andExpect(jsonPath("$.items[0].hojaUsada").value("base"))
                .andExpect(jsonPath("$.items[1].clics").value(2))
                .andExpect(jsonPath("$.items[1].hojaUsada").value("adaptada"))
                .andExpect(jsonPath("$.items[1].tipo").value("practica"))
                .andExpect(jsonPath("$.items[1].ciudad").value("Medellín"));

        // Búsqueda sin tildes ni mayúsculas, y orden por cargo
        mockMvc.perform(get("/api/v1/postulaciones").param("q", "INFORMATICA").with(persona))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].empresa").value("Empresa Ficticia"));
        mockMvc.perform(get("/api/v1/postulaciones").param("orden", "cargoZA").with(persona))
                .andExpect(jsonPath("$.items[0].cargo").value("Practicante de Ingeniería Informática"));
        mockMvc.perform(get("/api/v1/postulaciones").param("orden", "otro").with(persona))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verDesdeElHistorialVuelveALaOfertaYSumaUnClic() throws Exception {
        String id = JsonPath.read(postular(practica, "{\"hoja\":\"adaptada\"}"), "$.postulacionId");

        mockMvc.perform(post("/api/v1/postulaciones/" + id + "/clics").with(persona).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlOriginal").value("https://empresa.example/ofertas/1"))
                .andExpect(jsonPath("$.postulacionId").value(id));
        mockMvc.perform(get("/api/v1/postulaciones").with(persona))
                .andExpect(jsonPath("$.items[0].clics").value(2))
                .andExpect(jsonPath("$.items[0].hojaUsada").value("adaptada"));
    }

    @Test
    void quitarSePuedeDeshacerYDespuesDeUnDiaSeBorra() throws Exception {
        String id = JsonPath.read(postular(practica, "{}"), "$.postulacionId");

        mockMvc.perform(delete("/api/v1/postulaciones/" + id).with(persona).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/postulaciones").with(persona)).andExpect(jsonPath("$.items", hasSize(0)));

        mockMvc.perform(post("/api/v1/postulaciones/" + id + "/restauracion").with(persona).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/postulaciones").with(persona)).andExpect(jsonPath("$.items", hasSize(1)));

        // Quitada hace dos días: la limpieza la borra del todo, con sus clics
        mockMvc.perform(delete("/api/v1/postulaciones/" + id).with(persona).with(csrf()));
        jdbc.update("UPDATE postulacion SET quitada_en = ? WHERE id = ?::uuid",
                Timestamp.from(Instant.now().minus(Duration.ofDays(2))), id);
        assertThat(postulaciones.borrarQuitadas()).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM postulacion WHERE id = ?::uuid", Long.class, id)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM clic_postulacion WHERE postulacion_id = ?::uuid", Long.class, id))
                .isZero();
    }

    @Test
    void volverAPostularDevuelveAlHistorialUnaQuitada() throws Exception {
        String id = JsonPath.read(postular(practica, "{}"), "$.postulacionId");
        mockMvc.perform(delete("/api/v1/postulaciones/" + id).with(persona).with(csrf()));

        postular(practica, "{}");
        mockMvc.perform(get("/api/v1/postulaciones").with(persona))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].clics").value(2));
    }

    @Test
    void nadieVeNiTocaElHistorialDeOtraPersona() throws Exception {
        String id = JsonPath.read(postular(practica, "{}"), "$.postulacionId");

        mockMvc.perform(get("/api/v1/postulaciones").with(otra)).andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(delete("/api/v1/postulaciones/" + id).with(otra).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("postulacion_no_encontrada"));
        mockMvc.perform(post("/api/v1/postulaciones/" + id + "/clics").with(otra).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/postulaciones/" + id + "/restauracion").with(otra).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaVacantesInexistentesHojasInvalidasYSinSesion() throws Exception {
        mockMvc.perform(post("/api/v1/vacantes/" + UUID.randomUUID() + "/clics").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("vacante_no_encontrada"));
        mockMvc.perform(post("/api/v1/vacantes/" + practica + "/clics").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"hoja\":\"otra\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/vacantes/" + practica + "/clics").with(persona)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/postulaciones")).andExpect(status().isUnauthorized());
    }
}
