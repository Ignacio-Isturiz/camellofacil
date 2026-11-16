package com.camellofacil.backend.perfil;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camellofacil.backend.PruebaIntegracion;
import com.camellofacil.backend.auth.app.UsuarioService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Modal del primer ingreso, edición del perfil y bloqueo de la API sin perfil completo. Datos ficticios. */
class PerfilIntegracionTest extends PruebaIntegracion {

    private static final String COMPLETAR_ESTUDIANTE = """
            {"tipo":"ESTUDIANTE","carrera":"datos","semestre":8,"versionTerminos":"1.0",
             "aceptaTerminos":true,"autorizaAnalisisIa":true}""";

    @Autowired
    UsuarioService usuarios;

    private RequestPostProcessor persona;

    @BeforeEach
    void nuevaPersona() {
        String oid = UUID.randomUUID().toString();
        usuarios.registrarAcceso(UsuarioService.PROVEEDOR_MICROSOFT, oid, "prueba." + oid + "@unaula.edu.co", "Persona Prueba");
        persona = oidcLogin().idToken(t -> t.claim("oid", oid));
    }

    @Test
    void sinCompletarElPerfilApareceIncompletoYLaApiSeBloquea() throws Exception {
        mockMvc.perform(get("/api/v1/perfil").with(persona))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completo").value(false))
                .andExpect(jsonPath("$.versionTerminosVigente").value("1.0"))
                .andExpect(jsonPath("$.datos").doesNotExist());

        mockMvc.perform(get("/api/v1/otra-cosa").with(persona))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("perfil_incompleto"));
    }

    @Test
    void lasCarrerasSonLasOchoDeUnaula() throws Exception {
        mockMvc.perform(get("/api/v1/perfil/carreras").with(persona))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[7].codigo").value("sociales"));
    }

    @Test
    void completarElModalDesbloqueaLaApi() throws Exception {
        mockMvc.perform(post("/api/v1/perfil").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(COMPLETAR_ESTUDIANTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completo").value(true))
                .andExpect(jsonPath("$.datos.carrera.nombre").value("Ingeniería en Ciencia de Datos e Inteligencia de Negocios"))
                .andExpect(jsonPath("$.datos.semestre").value(8))
                .andExpect(jsonPath("$.terminos.version").value("1.0"))
                .andExpect(jsonPath("$.analisisIa.version").value("1.0"));

        // Ya no se bloquea: la ruta no existe, así que llega hasta el 404.
        mockMvc.perform(get("/api/v1/otra-cosa").with(persona)).andExpect(status().isNotFound());

        // No se puede completar dos veces.
        mockMvc.perform(post("/api/v1/perfil").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(COMPLETAR_ESTUDIANTE))
                .andExpect(status().isConflict());
    }

    @Test
    void elModalExigeConsentimientosYSemestreDeEstudiante() throws Exception {
        String sinIa = COMPLETAR_ESTUDIANTE.replace("\"autorizaAnalisisIa\":true", "\"autorizaAnalisisIa\":false");
        mockMvc.perform(post("/api/v1/perfil").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(sinIa))
                .andExpect(status().isBadRequest());

        String sinSemestre = COMPLETAR_ESTUDIANTE.replace("\"semestre\":8,", "");
        mockMvc.perform(post("/api/v1/perfil").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(sinSemestre))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("datos_invalidos"));

        String carreraFalsa = COMPLETAR_ESTUDIANTE.replace("\"datos\"", "\"medicina\"");
        mockMvc.perform(post("/api/v1/perfil").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(carreraFalsa))
                .andExpect(status().isBadRequest());

        String terminosViejos = COMPLETAR_ESTUDIANTE.replace("\"1.0\"", "\"0.9\"");
        mockMvc.perform(post("/api/v1/perfil").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(terminosViejos))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elEgresadoNoGuardaSemestreYPuedeEditarPreferencias() throws Exception {
        mockMvc.perform(post("/api/v1/perfil").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(COMPLETAR_ESTUDIANTE.replace("ESTUDIANTE", "EGRESADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.tipo").value("EGRESADO"))
                .andExpect(jsonPath("$.datos.semestre").doesNotExist());

        mockMvc.perform(put("/api/v1/perfil").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"EGRESADO","carrera":"sociales",
                                 "modalidades":["REMOTO","HIBRIDO"],"disponibilidad":"MEDIO_TIEMPO"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.carrera.codigo").value("sociales"))
                .andExpect(jsonPath("$.datos.modalidades.length()").value(2))
                .andExpect(jsonPath("$.datos.disponibilidad").value("MEDIO_TIEMPO"));
    }

    @Test
    void revocarLaAutorizacionDeIaNoQuitaElPerfil() throws Exception {
        mockMvc.perform(post("/api/v1/perfil").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(COMPLETAR_ESTUDIANTE))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/legal/analisis-ia").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":false}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/perfil").with(persona))
                .andExpect(jsonPath("$.completo").value(true))
                .andExpect(jsonPath("$.analisisIa").doesNotExist());
    }

    @Test
    void sinSesionNoHayPerfil() throws Exception {
        mockMvc.perform(get("/api/v1/perfil")).andExpect(status().isUnauthorized());
    }
}
