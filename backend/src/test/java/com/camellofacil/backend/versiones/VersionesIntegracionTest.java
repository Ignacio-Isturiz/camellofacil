package com.camellofacil.backend.versiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camellofacil.backend.ExtractorIaDePrueba;
import com.camellofacil.backend.PruebaIntegracion;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Plantillas, versiones y borradores de la hoja de vida, con Postgres real. */
class VersionesIntegracionTest extends PruebaIntegracion {

    private static final String VALORES = """
            {"nombre":"Ana Prueba Ficticia","telefono":"300 123 4567","ciudad":"Medellín","perfil":"Estudiante de datos",
             "habilidades":"Python, SQL",
             "experiencia":[{"id":"e1","cargo":"Monitora","organizacion":"Universidad de Ejemplo","inicio":"2024-02","fin":"","descripcion":"Apoyé prácticas"}],
             "educacion":[{"id":"d1","titulo":"Ingeniería de Sistemas","institucion":"Universidad de Ejemplo","inicio":"2021-01","fin":""}]}""";

    @Autowired UsuarioService usuarios;
    @Autowired PerfilService perfiles;
    @Autowired ConsentimientoService consentimientos;
    @Autowired HojaDeVidaService hojas;
    @Autowired AnalisisHojaService analisis;
    @Autowired JsonMapper json;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    private record Persona(UUID id, RequestPostProcessor sesion) {}

    private Persona nuevaPersona(boolean conHojaConfirmada) throws Exception {
        String oid = UUID.randomUUID().toString();
        UsuarioActual u = usuarios.registrarAcceso(UsuarioService.PROVEEDOR_MICROSOFT, oid, "ver." + oid + "@unaula.edu.co", "Ana Prueba");
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
                    "Ana Prueba Ficticia", "300 123 4567", "Medellín", "Estudiante de datos", "Python, SQL",
                    List.of(new ValorExperiencia("e1", "Monitora", "Universidad de Ejemplo", "2024-02", "", "Apoyé prácticas")),
                    List.of(new ValorEducacion("d1", "Ingeniería de Sistemas", "Universidad de Ejemplo", "2021-01", ""))));
            hojas.confirmar(u.id());
        }
        return p;
    }

    private JsonNode leer(org.springframework.test.web.servlet.MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    @Test
    void catalogoDePlantillas_conDetalleYGuias() throws Exception {
        Persona p = nuevaPersona(false);
        mockMvc.perform(get("/api/v1/plantillas").with(p.sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value("harvard-clasica"))
                .andExpect(jsonPath("$[0].categoria").value("harvard"));
        mockMvc.perform(get("/api/v1/plantillas/ats-compacta").with(p.sesion()))
                .andExpect(jsonPath("$.guias.length()").value(5));
        mockMvc.perform(get("/api/v1/plantillas/no-existe").with(p.sesion()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/plantillas")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinHojaConfirmada_noHayVistaPreviaNiVersiones() throws Exception {
        Persona p = nuevaPersona(false);
        mockMvc.perform(get("/api/v1/plantillas/harvard-clasica/vista-previa").with(p.sesion()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("sin_hoja"));
        mockMvc.perform(get("/api/v1/hoja-de-vida/versiones").with(p.sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void vistaPrevia_ordenaLasSeccionesSegunLaPlantillaYSoloUsaLoQueEscribioLaPersona() throws Exception {
        Persona p = nuevaPersona(true);
        JsonNode harvard = leer(mockMvc.perform(get("/api/v1/plantillas/harvard-clasica/vista-previa?idioma=en").with(p.sesion()))
                .andExpect(status().isOk()).andReturn());
        assertThat(harvard.get("nombre").asString()).isEqualTo("Ana Prueba Ficticia");
        assertThat(harvard.get("secciones").get(1).get("id").asString()).isEqualTo("educacion");
        assertThat(harvard.get("secciones").get(1).get("titulo").asString()).isEqualTo("Education");
        assertThat(harvard.get("secciones").get(2).get("bloques").get(0).asString()).contains("Monitora").contains("Present");

        JsonNode ats = leer(mockMvc.perform(get("/api/v1/plantillas/ats-clasica/vista-previa").with(p.sesion())).andReturn());
        assertThat(ats.get("secciones").get(1).get("id").asString()).isEqualTo("experiencia");

        mockMvc.perform(get("/api/v1/plantillas/harvard-clasica/vista-previa?idioma=fr").with(p.sesion()))
                .andExpect(status().isNotFound());
    }

    @Test
    void recorridoCompleto_baseBorradorVersionDuplicadoRenombreYEliminacion() throws Exception {
        Persona p = nuevaPersona(true);

        // La versión base se crea sola con los datos confirmados y no se elimina.
        JsonNode lista = leer(mockMvc.perform(get("/api/v1/hoja-de-vida/versiones").with(p.sesion()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eliminable").value(false))
                .andExpect(jsonPath("$[0].tipo").value("base")).andReturn());
        String baseId = lista.get(0).get("id").asString();
        mockMvc.perform(delete("/api/v1/hoja-de-vida/versiones/" + baseId).with(p.sesion()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("no_eliminable"));

        // Editor: borrador desde una plantilla, guardado y paso a versión.
        JsonNode borrador = leer(mockMvc.perform(post("/api/v1/hoja-de-vida/borradores").with(p.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plantillaId\":\"harvard-clasica\",\"idioma\":\"es\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valores.nombre").value("Ana Prueba Ficticia"))
                .andExpect(jsonPath("$.correo").value(org.hamcrest.Matchers.startsWith("ver."))).andReturn());
        String borradorId = borrador.get("id").asString();

        mockMvc.perform(put("/api/v1/hoja-de-vida/borradores/" + borradorId).with(p.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALORES.replace("Estudiante de datos", "Perfil editado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valores.perfil").value("Perfil editado"))
                .andExpect(jsonPath("$.guardadoEn").isNotEmpty());

        JsonNode nueva = leer(mockMvc.perform(post("/api/v1/hoja-de-vida/borradores/" + borradorId + "/version")
                        .with(p.sesion()).with(csrf()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.eliminable").value(true))
                // Lo que se arma en el editor no es «la base»: se ve como «editada».
                .andExpect(jsonPath("$.tipo").value("editada")).andReturn());
        String nuevaId = nueva.get("id").asString();
        JsonNode doc = leer(mockMvc.perform(get("/api/v1/hoja-de-vida/versiones/" + nuevaId + "/documento").with(p.sesion()))
                .andExpect(status().isOk()).andReturn());
        assertThat(doc.get("secciones").get(0).get("bloques").get(0).asString()).isEqualTo("Perfil editado");

        // Una versión se vuelve a editar desde su borrador.
        mockMvc.perform(post("/api/v1/hoja-de-vida/borradores").with(p.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"versionId\":\"" + nuevaId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plantillaId").value("harvard-clasica"))
                .andExpect(jsonPath("$.valores.perfil").value("Perfil editado"));

        mockMvc.perform(patch("/api/v1/hoja-de-vida/versiones/" + nuevaId).with(p.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Para prácticas\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Para prácticas"));
        mockMvc.perform(patch("/api/v1/hoja-de-vida/versiones/" + nuevaId).with(p.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/hoja-de-vida/versiones/" + nuevaId + "/duplicado").with(p.sesion()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Copia de Para prácticas"));

        // Cambiar la base: la anterior pasa a poder eliminarse.
        mockMvc.perform(post("/api/v1/hoja-de-vida/versiones/" + nuevaId + "/base").with(p.sesion()).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/hoja-de-vida/versiones/" + nuevaId).with(p.sesion()).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/hoja-de-vida/versiones/" + baseId).with(p.sesion()).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/hoja-de-vida/versiones").with(p.sesion()))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void nadieAlcanzaLasVersionesNiLosBorradoresDeOtraPersona() throws Exception {
        Persona dueña = nuevaPersona(true);
        Persona intrusa = nuevaPersona(true);
        String versionId = leer(mockMvc.perform(get("/api/v1/hoja-de-vida/versiones").with(dueña.sesion())).andReturn())
                .get(0).get("id").asString();
        String borradorId = leer(mockMvc.perform(post("/api/v1/hoja-de-vida/borradores").with(dueña.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plantillaId\":\"ats-clasica\"}")).andReturn())
                .get("id").asString();

        mockMvc.perform(get("/api/v1/hoja-de-vida/versiones/" + versionId + "/documento").with(intrusa.sesion()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/hoja-de-vida/versiones/" + versionId).with(intrusa.sesion()).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/hoja-de-vida/borradores/" + borradorId).with(intrusa.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALORES))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/hoja-de-vida/borradores").with(intrusa.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"versionId\":\"" + versionId + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validaLasEntradas_fechasMalEscritasPlantillaFaltanteOInexistente() throws Exception {
        Persona p = nuevaPersona(true);
        String borradorId = leer(mockMvc.perform(post("/api/v1/hoja-de-vida/borradores").with(p.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plantillaId\":\"ats-clasica\"}")).andReturn())
                .get("id").asString();

        mockMvc.perform(put("/api/v1/hoja-de-vida/borradores/" + borradorId).with(p.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALORES.replace("2024-02", "2024-13")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/hoja-de-vida/borradores").with(p.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("datos_invalidos"));
        mockMvc.perform(post("/api/v1/hoja-de-vida/borradores").with(p.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plantillaId\":\"no-existe\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinAutorizacionDeIaLaHojaConfirmadaSigueSiendoSuya() throws Exception {
        // Revocar la IA no borra versiones ya armadas: son datos de la persona.
        Persona p = nuevaPersona(true);
        mockMvc.perform(get("/api/v1/hoja-de-vida/versiones").with(p.sesion())).andExpect(jsonPath("$.length()").value(1));
        consentimientos.revocar(p.id(), TipoConsentimiento.ANALISIS_IA);
        mockMvc.perform(get("/api/v1/hoja-de-vida/versiones").with(p.sesion()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void descargaUnaVersionEnPdfYWord_soloSuDuena() throws Exception {
        Persona dueña = nuevaPersona(true);
        Persona intrusa = nuevaPersona(true);
        String id = leer(mockMvc.perform(get("/api/v1/hoja-de-vida/versiones").with(dueña.sesion())).andReturn()).get(0).get("id").asString();

        byte[] pdf = mockMvc.perform(get("/api/v1/hoja-de-vida/versiones/" + id + "/descarga?formato=pdf").with(dueña.sesion()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("application/pdf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", org.hamcrest.Matchers.containsString("hoja-de-vida.pdf")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

        byte[] docx = mockMvc.perform(get("/api/v1/hoja-de-vida/versiones/" + id + "/descarga?formato=docx").with(dueña.sesion()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(docx, 0, 2)).isEqualTo("PK");

        mockMvc.perform(get("/api/v1/hoja-de-vida/versiones/" + id + "/descarga?formato=exe").with(dueña.sesion()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/hoja-de-vida/versiones/" + id + "/descarga").with(intrusa.sesion()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/hoja-de-vida/versiones/" + id + "/descarga")).andExpect(status().isUnauthorized());
    }

    @Test
    void unBorradorSeVuelveAAbrirConLoGuardado_sinCrearOtro_ySoloSuDuena() throws Exception {
        Persona dueña = nuevaPersona(true);
        Persona intrusa = nuevaPersona(true);
        String id = leer(mockMvc.perform(post("/api/v1/hoja-de-vida/borradores").with(dueña.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plantillaId\":\"harvard-clasica\"}")).andReturn())
                .get("id").asString();
        mockMvc.perform(put("/api/v1/hoja-de-vida/borradores/" + id).with(dueña.sesion()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALORES.replace("Estudiante de datos", "Perfil que escribí")))
                .andExpect(status().isOk());

        // Recargar el editor: se pide el mismo borrador y llega con lo que se había guardado.
        mockMvc.perform(get("/api/v1/hoja-de-vida/borradores/" + id).with(dueña.sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.valores.perfil").value("Perfil que escribí"))
                .andExpect(jsonPath("$.guardadoEn").isNotEmpty());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM borrador_hoja WHERE usuario_id = ?", Integer.class, dueña.id())).isEqualTo(1);

        mockMvc.perform(get("/api/v1/hoja-de-vida/borradores/" + id).with(intrusa.sesion())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/hoja-de-vida/borradores/" + id)).andExpect(status().isUnauthorized());
    }
}
