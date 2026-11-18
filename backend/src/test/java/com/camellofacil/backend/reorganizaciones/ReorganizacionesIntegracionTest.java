package com.camellofacil.backend.reorganizaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camellofacil.backend.ExtractorIaDePrueba;
import com.camellofacil.backend.PruebaIntegracion;
import com.camellofacil.backend.ReorganizadorIaDePrueba;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reorganizar a Harvard o ATS: plan de cambios, IA verificada, alternar cambios sin IA y guardar como versión. */
class ReorganizacionesIntegracionTest extends PruebaIntegracion {

    @Autowired UsuarioService usuarios;
    @Autowired PerfilService perfiles;
    @Autowired ConsentimientoService consentimientos;
    @Autowired HojaDeVidaService hojas;
    @Autowired AnalisisHojaService analisis;
    @Autowired JsonMapper json;

    private record Persona(UUID id, RequestPostProcessor sesion) {}

    @BeforeEach
    void preparar() {
        ReorganizadorIaDePrueba.LLAMADAS.set(0);
        ReorganizadorIaDePrueba.ULTIMA_HOJA.set(null);
        // Redacciona el perfil y e2 con lo que ya hay; e1 intenta colar «Power BI», que no está en la hoja.
        ReorganizadorIaDePrueba.RESPUESTA.set(new PropuestaIa(
                "Estudiante de datos con enfoque en análisis.", List.of("Kubernetes"),
                List.of(new ExperienciaIa("e1", "Analicé datos con Power BI."),
                        new ExperienciaIa("e2", "Reduje 30% el tiempo de reporte con dashboards. Presenté los resultados a la gerencia.")),
                List.of()));
    }

    private Persona persona(boolean confirmada) throws Exception {
        String oid = UUID.randomUUID().toString();
        UsuarioActual u = usuarios.registrarAcceso(UsuarioService.PROVEEDOR_MICROSOFT, oid, "reo." + oid + "@unaula.edu.co", "Ana Prueba");
        perfiles.completar(u.id(), new DatosBasicos(TipoPersona.ESTUDIANTE, "datos", 7));
        if (confirmada) {
            ExtractorIaDePrueba.RESPUESTA.set(new Extraccion("es", "Ana Prueba", "300 000 0000", "Medellin", "Perfil", "Python", List.of(), List.of()));
            while (analisis.procesarSiguiente()) {
                // vaciar la cola de otras pruebas
            }
            hojas.subir(u.id(), "cv.pdf", DocumentosDePrueba.pdf(DocumentosDePrueba.LINEAS));
            assertThat(analisis.procesarSiguiente()).isTrue();
            hojas.corregir(u.id(), new ValoresHoja("Ana Prueba", "300 000 0000", "Medellín", "Estudiante de datos enfocada en análisis.", "Python, SQL",
                    List.of(new ValorExperiencia("e1", "Monitora", "Universidad de Ejemplo", "2024-02", "2024-11", "Apoyé las prácticas del laboratorio."),
                            new ValorExperiencia("e2", "Practicante", "Empresa Ejemplo", "2025-06", "",
                                    "Construí dashboards que redujeron 30% el tiempo de reporte. Presenté los resultados a la gerencia.")),
                    List.of(new ValorEducacion("d1", "Bachiller", "Colegio de Ejemplo", "2018-01", "2022-12"),
                            new ValorEducacion("d2", "Ingeniería de Sistemas", "Universidad de Ejemplo", "2023-01", ""))));
            hojas.confirmar(u.id());
        }
        return new Persona(u.id(), oidcLogin().idToken(t -> t.claim("oid", oid)));
    }

    private JsonNode leer(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private MvcResult proponer(Persona p, String formato, String idioma) throws Exception {
        return mockMvc.perform(post("/api/v1/hoja-de-vida/reorganizaciones").with(p.sesion()).with(csrf())
                .contentType("application/json").content("{\"formato\":\"" + formato + "\",\"idioma\":\"" + idioma + "\"}")).andReturn();
    }

    private static List<String> ids(JsonNode secciones) {
        return secciones.valueStream().map(s -> s.get("id").asString()).toList();
    }

    private static List<String> idsDeCambios(JsonNode cambios) {
        return cambios.valueStream().map(c -> c.get("id").asString()).toList();
    }

    @Test
    void proponeAHarvard_conIaVerificada_y_nadaInventado() throws Exception {
        Persona p = persona(true);

        JsonNode r = leer(mockMvc.perform(post("/api/v1/hoja-de-vida/reorganizaciones").with(p.sesion()).with(csrf())
                        .contentType("application/json").content("{\"formato\":\"harvard\",\"idioma\":\"es\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.formato").value("harvard")).andReturn());

        // Los cambios son los que respalda la hoja: «Power BI» de e1 y «Kubernetes» no llegan.
        assertThat(idsDeCambios(r.get("cambios"))).containsExactly(
                "orden-secciones", "orden-experiencia", "orden-educacion", "perfil", "exp-e2", "vin-e2");
        assertThat(r.toString()).doesNotContain("Power BI").doesNotContain("Kubernetes");
        assertThat(r.get("cambios").valueStream()).allMatch(c -> c.get("aplicado").asBoolean());

        // Original (orden ATS de la hoja base) frente a la propuesta (Harvard: estudios primero, lo reciente arriba).
        assertThat(ids(r.get("original").get("secciones"))).containsExactly("perfil", "experiencia", "educacion", "habilidades");
        assertThat(ids(r.get("propuesta").get("secciones"))).containsExactly("perfil", "educacion", "experiencia", "habilidades");
        assertThat(r.get("propuesta").get("secciones").get(2).get("bloques").get(0).asString()).startsWith("Practicante");
        assertThat(r.get("propuesta").get("secciones").get(2).get("bloques").get(0).asString()).contains("• Reduje 30%");
        assertThat(r.get("original").get("secciones").get(1).get("bloques").get(0).asString()).startsWith("Monitora");

        // La IA no recibió datos de contacto.
        assertThat(ReorganizadorIaDePrueba.ULTIMA_HOJA.get().nombre()).isEmpty();
        assertThat(ReorganizadorIaDePrueba.ULTIMA_HOJA.get().telefono()).isEmpty();
    }

    @Test
    void alternarUnCambioNoLlamaALaIa_yCambiarDeFormatoOIdiomaTampoco() throws Exception {
        Persona p = persona(true);
        JsonNode r = leer(proponer(p, "harvard", "es"));
        String id = r.get("id").asString();
        assertThat(ReorganizadorIaDePrueba.LLAMADAS.get()).isEqualTo(1);

        // Revertir el orden de las secciones: la propuesta vuelve al orden original en esa parte.
        JsonNode sin = leer(mockMvc.perform(put("/api/v1/hoja-de-vida/reorganizaciones/" + id + "/cambios/orden-secciones")
                        .with(p.sesion()).with(csrf()).contentType("application/json").content("{\"aplicado\":false}"))
                .andExpect(status().isOk()).andReturn());
        assertThat(ids(sin.get("propuesta").get("secciones"))).containsExactly("perfil", "experiencia", "educacion", "habilidades");
        assertThat(sin.get("cambios").valueStream().filter(c -> c.get("id").asString().equals("orden-secciones")).findFirst().orElseThrow()
                .get("aplicado").asBoolean()).isFalse();
        // Los demás siguen aplicados.
        assertThat(sin.get("propuesta").get("secciones").get(1).get("bloques").get(0).asString()).startsWith("Practicante");

        mockMvc.perform(put("/api/v1/hoja-de-vida/reorganizaciones/" + id + "/cambios/no-existe")
                        .with(p.sesion()).with(csrf()).contentType("application/json").content("{\"aplicado\":false}"))
                .andExpect(status().isNotFound());

        // Otro formato e idioma con la misma hoja reutilizan la redacción: no hay otra llamada a la IA.
        JsonNode ats = leer(proponer(p, "ats", "en"));
        assertThat(ReorganizadorIaDePrueba.LLAMADAS.get()).isEqualTo(1);
        assertThat(idsDeCambios(ats.get("cambios"))).doesNotContain("orden-secciones").contains("perfil", "exp-e2");
        assertThat(ats.get("propuesta").get("secciones").get(0).get("titulo").asString()).isEqualTo("Profile");
    }

    @Test
    void aceptarGuardaUnaVersionReorganizada_unaSolaVez_yDescartarLaQuita() throws Exception {
        Persona p = persona(true);
        String id = leer(proponer(p, "harvard", "es")).get("id").asString();

        String versionId = leer(mockMvc.perform(post("/api/v1/hoja-de-vida/reorganizaciones/" + id + "/aceptacion").with(p.sesion()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("reorganizada"))
                .andExpect(jsonPath("$.nombre").value(org.hamcrest.Matchers.containsString("Harvard"))).andReturn())
                .get("id").asString();
        mockMvc.perform(post("/api/v1/hoja-de-vida/reorganizaciones/" + id + "/aceptacion").with(p.sesion()).with(csrf()))
                .andExpect(jsonPath("$.id").value(versionId));

        // La versión lleva el documento reorganizado y se puede descargar.
        JsonNode doc = leer(mockMvc.perform(get("/api/v1/hoja-de-vida/versiones/" + versionId + "/documento").with(p.sesion())).andReturn());
        assertThat(ids(doc.get("secciones"))).containsExactly("perfil", "educacion", "experiencia", "habilidades");
        mockMvc.perform(get("/api/v1/hoja-de-vida/versiones/" + versionId + "/descarga?formato=pdf").with(p.sesion())).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/hoja-de-vida/reorganizaciones/" + id).with(p.sesion()).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/hoja-de-vida/reorganizaciones/" + id + "/aceptacion").with(p.sesion()).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void sinIaLaPropuestaSeArmaIgualConElOrden_yAvisa() throws Exception {
        Persona sinFalla = persona(true);
        ReorganizadorIaDePrueba.RESPUESTA.set(null);
        JsonNode falla = leer(mockMvc.perform(post("/api/v1/hoja-de-vida/reorganizaciones").with(sinFalla.sesion()).with(csrf())
                        .contentType("application/json").content("{\"formato\":\"harvard\",\"idioma\":\"es\"}"))
                .andExpect(status().isCreated()).andReturn());
        assertThat(idsDeCambios(falla.get("cambios"))).containsExactly("orden-secciones", "orden-experiencia", "orden-educacion", "vin-e2");
        assertThat(falla.get("avisos").toString()).contains("No pudimos mejorar la redacción");

        // Sin autorización de IA: no se llama a la IA y se avisa cómo activarla.
        ReorganizadorIaDePrueba.LLAMADAS.set(0);
        ReorganizadorIaDePrueba.RESPUESTA.set(new PropuestaIa("x", List.of(), List.of(), List.of()));
        Persona sinAutorizacion = persona(true);
        consentimientos.revocar(sinAutorizacion.id(), TipoConsentimiento.ANALISIS_IA);
        JsonNode sin = leer(proponer(sinAutorizacion, "ats", "es"));
        assertThat(ReorganizadorIaDePrueba.LLAMADAS.get()).isZero();
        assertThat(sin.get("avisos").toString()).contains("autorización de análisis con IA");
    }

    @Test
    void despuesDeSeisLlamadasALaIaEnUnDiaSigueFuncionandoSoloConElOrden() throws Exception {
        Persona p = persona(true);
        for (int i = 0; i < 6; i++) {
            // Cada cambio de la hoja base obliga a pedir una redacción nueva.
            Thread.sleep(5);
            hojas.corregir(p.id(), new ValoresHoja("Ana Prueba", "300 000 0000", "Medellín", "Perfil número " + i + " de la persona.", "Python, SQL",
                    List.of(new ValorExperiencia("e1", "Monitora", "Universidad de Ejemplo", "2024-02", "2024-11", "Apoyé las prácticas del laboratorio."),
                            new ValorExperiencia("e2", "Practicante", "Empresa Ejemplo", "2025-06", "", "Construí dashboards. Presenté resultados.")),
                    List.of(new ValorEducacion("d1", "Ingeniería de Sistemas", "Universidad de Ejemplo", "2023-01", ""))));
            hojas.confirmar(p.id());
            assertThat(proponer(p, "harvard", "es").getResponse().getStatus()).isEqualTo(201);
        }
        assertThat(ReorganizadorIaDePrueba.LLAMADAS.get()).isEqualTo(6);

        Thread.sleep(5);
        hojas.corregir(p.id(), new ValoresHoja("Ana Prueba", "300 000 0000", "Medellín", "Otro perfil distinto de la persona.", "Python, SQL",
                List.of(new ValorExperiencia("e1", "Monitora", "Universidad de Ejemplo", "2024-02", "2024-11", "Apoyé las prácticas del laboratorio.")),
                List.of(new ValorEducacion("d1", "Ingeniería de Sistemas", "Universidad de Ejemplo", "2023-01", ""))));
        hojas.confirmar(p.id());
        JsonNode r = leer(proponer(p, "harvard", "es"));
        assertThat(ReorganizadorIaDePrueba.LLAMADAS.get()).isEqualTo(6);
        assertThat(r.get("avisos").toString()).contains("6 veces hoy");
        assertThat(idsDeCambios(r.get("cambios"))).contains("orden-secciones");
    }

    @Test
    void validaLasEntradas_yNadieAlcanzaLasPropuestasDeOtraPersona() throws Exception {
        Persona dueña = persona(true);
        Persona intrusa = persona(true);
        Persona sinHoja = persona(false);

        assertThat(proponer(dueña, "pdf", "es").getResponse().getStatus()).isEqualTo(400);
        assertThat(proponer(dueña, "harvard", "fr").getResponse().getStatus()).isEqualTo(400);
        assertThat(proponer(sinHoja, "harvard", "es").getResponse().getStatus()).isEqualTo(404);

        String id = leer(proponer(dueña, "harvard", "es")).get("id").asString();
        mockMvc.perform(put("/api/v1/hoja-de-vida/reorganizaciones/" + id + "/cambios/perfil").with(intrusa.sesion()).with(csrf())
                        .contentType("application/json").content("{\"aplicado\":false}")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/hoja-de-vida/reorganizaciones/" + id + "/aceptacion").with(intrusa.sesion()).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/hoja-de-vida/reorganizaciones/" + id).with(intrusa.sesion()).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/hoja-de-vida/reorganizaciones").with(csrf()).contentType("application/json")
                .content("{\"formato\":\"harvard\",\"idioma\":\"es\"}")).andExpect(status().isUnauthorized());
    }
}
