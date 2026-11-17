package com.camellofacil.backend.recomendaciones;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
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

/** Inicio recomienda según la carrera del perfil, de a 4 por página, y no repite lo ya postulado. */
class InicioIntegracionTest extends PruebaIntegracion {

    @Autowired
    UsuarioService usuarios;

    @Autowired
    PerfilService perfiles;

    @Autowired
    JdbcTemplate jdbc;

    private UUID sistemas;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM vacante");
        sistemas = vacante("Practicante de sistemas", "{informatica}", 1);
        vacante("Practicante de analítica", "{datos,informatica}", 10);
        vacante("Practicante jurídico", "{derecho}", 0);
    }

    private RequestPostProcessor persona(String carrera) {
        String oid = UUID.randomUUID().toString();
        UsuarioActual u = usuarios.registrarAcceso(
                UsuarioService.PROVEEDOR_MICROSOFT, oid, "ini." + oid + "@unaula.edu.co", "Persona Prueba");
        perfiles.completar(u.id(), new DatosBasicos(TipoPersona.ESTUDIANTE, carrera, 6));
        return oidcLogin().idToken(t -> t.claim("oid", oid));
    }

    private UUID vacante(String cargo, String carreras, int dias) {
        UUID id = UUID.randomUUID();
        Timestamp publicada = Timestamp.from(Instant.now().minus(Duration.ofDays(dias)));
        jdbc.update("""
                INSERT INTO vacante (id, fuente, id_externo, huella, cargo, empresa, ciudad, modalidad, tipo,
                                     url_postulacion, carreras, publicada_en, vista_en, creada_en)
                VALUES (?, 'prueba', ?, ?, ?, 'Empresa Ficticia', 'Medellín', 'PRESENCIAL', 'PRACTICA',
                        'https://empresa.example/1', ?::text[], ?, ?, ?)""",
                id, id.toString(), id.toString().replace("-", "") + "0".repeat(32), cargo, carreras,
                publicada, publicada, publicada);
        return id;
    }

    @Test
    void recomiendaSegunLaCarreraDelPerfil() throws Exception {
        mockMvc.perform(get("/api/v1/inicio").with(persona("informatica")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendadas", hasSize(2)))
                .andExpect(jsonPath("$.recomendadas[0].vacante.cargo").value("Practicante de sistemas"))
                .andExpect(jsonPath("$.recomendadas[0].nueva").value(true))
                .andExpect(jsonPath("$.recomendadas[0].razones").doesNotExist())
                .andExpect(jsonPath("$.recomendadas[1].nueva").value(false))
                .andExpect(jsonPath("$.vacantesNuevas").value(1))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(1));

        mockMvc.perform(get("/api/v1/inicio").with(persona("derecho")))
                .andExpect(jsonPath("$.recomendadas", hasSize(1)))
                .andExpect(jsonPath("$.recomendadas[0].vacante.cargo").value("Practicante jurídico"));
        mockMvc.perform(get("/api/v1/inicio").with(persona("economia")))
                .andExpect(jsonPath("$.recomendadas", hasSize(0)))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    void paginaDeACuatro() throws Exception {
        for (int i = 0; i < 7; i++) vacante("Desarrollador web " + i, "{informatica}", 2 + i);
        RequestPostProcessor p = persona("informatica");

        mockMvc.perform(get("/api/v1/inicio").with(p))
                .andExpect(jsonPath("$.recomendadas", hasSize(4)))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.total").value(9))
                .andExpect(jsonPath("$.recomendadas[0].vacante.cargo").value("Practicante de sistemas"));
        mockMvc.perform(get("/api/v1/inicio").param("pagina", "2").with(p))
                .andExpect(jsonPath("$.recomendadas", hasSize(1)))
                .andExpect(jsonPath("$.pagina").value(2));
        // Una página que ya no existe devuelve la última
        mockMvc.perform(get("/api/v1/inicio").param("pagina", "9").with(p))
                .andExpect(jsonPath("$.pagina").value(2));
    }

    @Test
    void noRecomiendaLoYaPostulado() throws Exception {
        RequestPostProcessor p = persona("informatica");
        mockMvc.perform(post("/api/v1/vacantes/" + sistemas + "/clics").with(p).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/inicio").with(p))
                .andExpect(jsonPath("$.recomendadas", hasSize(1)))
                .andExpect(jsonPath("$.recomendadas[0].vacante.cargo").value("Practicante de analítica"));
    }

    @Test
    void sinSesionNoHayInicio() throws Exception {
        mockMvc.perform(get("/api/v1/inicio")).andExpect(status().isUnauthorized());
    }
}
