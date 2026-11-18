package com.camellofacil.backend.adaptaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camellofacil.backend.AdaptadorIaDePrueba;
import com.camellofacil.backend.ExtractorIaDePrueba;
import com.camellofacil.backend.PruebaIntegracion;
import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.ExperienciaIa;
import com.camellofacil.backend.adaptaciones.app.AdaptadorIa.PropuestaIa;
import com.camellofacil.backend.auth.app.UsuarioActual;
import com.camellofacil.backend.auth.app.UsuarioService;
import com.camellofacil.backend.cv.DocumentosDePrueba;
import com.camellofacil.backend.cv.app.AnalisisHojaService;
import com.camellofacil.backend.cv.app.ExtractorIa.Extraccion;
import com.camellofacil.backend.cv.app.HojaDeVidaService;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorEducacion;
import com.camellofacil.backend.cv.app.ModelosHoja.ValorExperiencia;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import com.camellofacil.backend.legal.app.ConsentimientoService;
import com.camellofacil.backend.legal.app.TipoConsentimiento;
import com.camellofacil.backend.perfil.app.ModelosPerfil.DatosBasicos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.TipoPersona;
import com.camellofacil.backend.perfil.app.PerfilService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Adaptar la hoja a una vacante: cupo diario, IA falsa, verificación anti-invención y guardado como versión. */
class AdaptacionesIntegracionTest extends PruebaIntegracion {

    @Autowired UsuarioService usuarios;
    @Autowired PerfilService perfiles;
    @Autowired ConsentimientoService consentimientos;
    @Autowired HojaDeVidaService hojas;
    @Autowired AnalisisHojaService analisis;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;

    private UUID vacante;

    private record Persona(UUID id, RequestPostProcessor sesion) {}

    @BeforeEach
    void preparar() {
        vacante = UUID.randomUUID();
        Timestamp ahora = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO vacante (id, fuente, id_externo, huella, cargo, empresa, ciudad, modalidad, tipo,
                                     url_postulacion, descripcion, publicada_en, vista_en, creada_en)
                VALUES (?, 'prueba', ?, ?, 'Practicante de datos', 'Empresa Ficticia', 'Medellín', 'HIBRIDO', 'PRACTICA',
                        'https://empresa.example/oferta', 'Buscamos a alguien con SQL y Power BI.', ?, ?, ?)""",
                vacante, vacante.toString(), vacante.toString().replace("-", "") + "1".repeat(32), ahora, ahora, ahora);
        AdaptadorIaDePrueba.LLAMADAS.set(0);
        AdaptadorIaDePrueba.ULTIMA_HOJA.set(null);
        // Reordena, destaca SQL, intenta colar Kubernetes y una descripción con cifra inventada; avisa lo que falta.
        AdaptadorIaDePrueba.RESPUESTA.set(new PropuestaIa(
                "Estudiante de datos enfocada en análisis con Python.",
                List.of("SQL", "Kubernetes", "Python"),
                List.of(new ExperienciaIa("e2", "Atención al cliente y manejo de caja."),
                        new ExperienciaIa("e1", "Atendí a 900 estudiantes con Power BI.")),
                List.of("La vacante pide Power BI y no aparece en tu hoja")));
    }

    private Persona nuevaPersona(boolean conHojaConfirmada) throws Exception {
        String oid = UUID.randomUUID().toString();
        UsuarioActual u = usuarios.registrarAcceso(UsuarioService.PROVEEDOR_MICROSOFT, oid, "ada." + oid + "@unaula.edu.co", "Ana Prueba");
        perfiles.completar(u.id(), new DatosBasicos(TipoPersona.ESTUDIANTE, "datos", 7));
        Persona p = new Persona(u.id(), oidcLogin().idToken(t -> t.claim("oid", oid)));
        if (conHojaConfirmada) {
            ExtractorIaDePrueba.RESPUESTA.set(new Extraccion("es", "Ana Prueba Ficticia", "300 123 4567", "Medellin", "Perfil", "Python",
                    List.of(), List.of()));
            while (analisis.procesarSiguiente()) {
                // vaciar la cola de otras pruebas
            }
            hojas.subir(u.id(), "cv.pdf", DocumentosDePrueba.pdf(DocumentosDePrueba.LINEAS));
            assertThat(analisis.procesarSiguiente()).isTrue();
            hojas.corregir(u.id(), new ValoresHoja(
                    "Ana Prueba Ficticia", "300 123 4567", "Medellín", "Estudiante de datos con análisis en Python.", "Python, SQL, Excel",
                    List.of(new ValorExperiencia("e1", "Monitora", "Universidad de Ejemplo", "2024-02", "2024-11", "Apoyé prácticas de laboratorio."),
                            new ValorExperiencia("e2", "Auxiliar", "Tienda Ejemplo", "2023-01", "2023-12", "Atención al cliente y manejo de caja.")),
                    List.of(new ValorEducacion("d1", "Ingeniería de Sistemas", "Universidad de Ejemplo", "2021-01", ""))));
            hojas.confirmar(u.id());
        }
        return p;
    }

    private JsonNode leer(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private MvcResult adaptar(Persona p) throws Exception {
        return mockMvc.perform(post("/api/v1/vacantes/" + vacante + "/adaptacion").with(p.sesion()).with(csrf())).andReturn();
    }

    @Test
    void adaptaSinInventar_yQuedaGuardadaComoVersion() throws Exception {
        Persona p = nuevaPersona(true);

        JsonNode a = leer(mockMvc.perform(post("/api/v1/vacantes/" + vacante + "/adaptacion").with(p.sesion()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restantesHoy").value(2))
                .andExpect(jsonPath("$.avisos[0]").value("La vacante pide Power BI y no aparece en tu hoja")).andReturn());

        String adaptada = a.get("adaptada").toString();
        // Lo que la IA coló no llega a la hoja: ni la habilidad ni la cifra inventadas.
        assertThat(adaptada).doesNotContain("Kubernetes").doesNotContain("900").doesNotContain("Power BI");
        // Lo permitido sí: habilidades y experiencias reordenadas, perfil reescrito con lo que ya había.
        assertThat(a.get("adaptada").get("secciones").get(0).get("bloques").get(0).asString())
                .isEqualTo("Estudiante de datos enfocada en análisis con Python.");
        assertThat(a.get("adaptada").get("secciones").get(1).get("bloques").get(0).asString()).startsWith("Auxiliar");
        assertThat(a.get("cambios").toString()).contains("redaccion").contains("movido").contains("destacado");
        // La hoja base no se toca.
        assertThat(a.get("original").get("secciones").get(1).get("bloques").get(0).asString()).startsWith("Monitora");

        // El proveedor de IA no recibe datos de contacto.
        assertThat(AdaptadorIaDePrueba.ULTIMA_HOJA.get().nombre()).isEmpty();
        assertThat(AdaptadorIaDePrueba.ULTIMA_HOJA.get().telefono()).isEmpty();
        assertThat(AdaptadorIaDePrueba.ULTIMA_HOJA.get().ciudad()).isEmpty();

        mockMvc.perform(get("/api/v1/vacantes/" + vacante + "/adaptacion").with(p.sesion()))
                .andExpect(jsonPath("$.adaptacion.id").value(a.get("id").asString()))
                .andExpect(jsonPath("$.restantesHoy").value(2));
        mockMvc.perform(get("/api/v1/adaptaciones/cupo").with(p.sesion()))
                .andExpect(jsonPath("$.usadas").value(1)).andExpect(jsonPath("$.limite").value(3));

        // Se puede descargar sin haberla guardado antes: PDF y Word.
        byte[] pdf = mockMvc.perform(get("/api/v1/adaptaciones/" + a.get("id").asString() + "/descarga?formato=pdf").with(p.sesion()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", org.hamcrest.Matchers.containsString("hoja-de-vida-adaptada.pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        mockMvc.perform(get("/api/v1/adaptaciones/" + a.get("id").asString() + "/descarga?formato=docx").with(p.sesion()))
                .andExpect(status().isOk());

        // Aceptar la guarda como versión «adaptada»; aceptarla otra vez no duplica.
        String versionId = leer(mockMvc.perform(post("/api/v1/adaptaciones/" + a.get("id").asString() + "/aceptacion")
                        .with(p.sesion()).with(csrf()).contentType("application/json").content("{\"postular\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("adaptada"))
                .andExpect(jsonPath("$.nombre").value(org.hamcrest.Matchers.containsString("Practicante de datos"))).andReturn())
                .get("id").asString();
        mockMvc.perform(post("/api/v1/adaptaciones/" + a.get("id").asString() + "/aceptacion").with(p.sesion()).with(csrf()))
                .andExpect(jsonPath("$.id").value(versionId));
        mockMvc.perform(get("/api/v1/hoja-de-vida/versiones").with(p.sesion()))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void despuesDeTresAdaptacionesElCuartoIntentoDaCuposAgotados() throws Exception {
        Persona p = nuevaPersona(true);
        for (int i = 0; i < 3; i++) {
            assertThat(adaptar(p).getResponse().getStatus()).isEqualTo(200);
        }
        mockMvc.perform(post("/api/v1/vacantes/" + vacante + "/adaptacion").with(p.sesion()).with(csrf()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("limite"));
        // Una cuarta llamada no llegó a gastarse en la IA.
        assertThat(AdaptadorIaDePrueba.LLAMADAS.get()).isEqualTo(3);
        mockMvc.perform(get("/api/v1/vacantes/" + vacante + "/adaptacion").with(p.sesion()))
                .andExpect(jsonPath("$.restantesHoy").value(0));
        // Regenerar reemplaza: sigue habiendo una sola adaptación para la vacante.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM adaptacion_hoja WHERE usuario_id = ?", Integer.class, p.id())).isEqualTo(1);
    }

    @Test
    void siLaIaFallaNoSeGastaElCupo() throws Exception {
        Persona p = nuevaPersona(true);
        AdaptadorIaDePrueba.RESPUESTA.set(null);

        mockMvc.perform(post("/api/v1/vacantes/" + vacante + "/adaptacion").with(p.sesion()).with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("ia_no_disponible"));
        mockMvc.perform(get("/api/v1/adaptaciones/cupo").with(p.sesion()))
                .andExpect(jsonPath("$.usadas").value(0));
        mockMvc.perform(get("/api/v1/vacantes/" + vacante + "/adaptacion").with(p.sesion()))
                .andExpect(jsonPath("$.adaptacion").doesNotExist())
                .andExpect(jsonPath("$.restantesHoy").value(3));
    }

    @Test
    void sinAutorizacionDeIaOSinHojaNoSeAdapta() throws Exception {
        Persona sinHoja = nuevaPersona(false);
        mockMvc.perform(post("/api/v1/vacantes/" + vacante + "/adaptacion").with(sinHoja.sesion()).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("sin_hoja"));

        Persona sinAutorizacion = nuevaPersona(true);
        consentimientos.revocar(sinAutorizacion.id(), TipoConsentimiento.ANALISIS_IA);
        mockMvc.perform(post("/api/v1/vacantes/" + vacante + "/adaptacion").with(sinAutorizacion.sesion()).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("sin_autorizacion_ia"));
        assertThat(AdaptadorIaDePrueba.LLAMADAS.get()).isZero();

        mockMvc.perform(post("/api/v1/vacantes/" + UUID.randomUUID() + "/adaptacion").with(sinAutorizacion.sesion()).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void nadieAlcanzaLasAdaptacionesDeOtraPersona() throws Exception {
        Persona dueña = nuevaPersona(true);
        Persona intrusa = nuevaPersona(true);
        String id = leer(adaptar(dueña)).get("id").asString();

        mockMvc.perform(get("/api/v1/vacantes/" + vacante + "/adaptacion").with(intrusa.sesion()))
                .andExpect(jsonPath("$.adaptacion").doesNotExist())
                .andExpect(jsonPath("$.restantesHoy").value(3));
        mockMvc.perform(get("/api/v1/adaptaciones/" + id + "/descarga").with(intrusa.sesion())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/adaptaciones/" + id).with(intrusa.sesion()).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/adaptaciones/" + id + "/aceptacion").with(intrusa.sesion()).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/adaptaciones/" + id).with(dueña.sesion()).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/adaptaciones/cupo")).andExpect(status().isUnauthorized());
    }

    @Test
    void elEditorSeAbreSobreLaHojaAdaptada_soloParaSuDuena() throws Exception {
        Persona dueña = nuevaPersona(true);
        Persona intrusa = nuevaPersona(true);
        String id = leer(adaptar(dueña)).get("id").asString();

        mockMvc.perform(post("/api/v1/hoja-de-vida/borradores").with(dueña.sesion()).with(csrf())
                        .contentType("application/json").content("{\"adaptacionId\":\"" + id + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plantillaId").value("ats-clasica"))
                // Trae lo adaptado (SQL primero), no la hoja base.
                .andExpect(jsonPath("$.valores.habilidades").value("SQL, Python, Excel"))
                .andExpect(jsonPath("$.valores.perfil").value("Estudiante de datos enfocada en análisis con Python."));

        mockMvc.perform(post("/api/v1/hoja-de-vida/borradores").with(intrusa.sesion()).with(csrf())
                        .contentType("application/json").content("{\"adaptacionId\":\"" + id + "\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/hoja-de-vida/borradores").with(dueña.sesion()).with(csrf())
                        .contentType("application/json").content("{\"adaptacionId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
    }
}
