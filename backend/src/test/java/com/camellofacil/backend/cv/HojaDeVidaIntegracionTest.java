package com.camellofacil.backend.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camellofacil.backend.ExtractorIaDePrueba;
import com.camellofacil.backend.PruebaIntegracion;
import com.camellofacil.backend.auth.app.UsuarioActual;
import com.camellofacil.backend.auth.app.UsuarioService;
import com.camellofacil.backend.cv.app.AnalisisHojaService;
import com.camellofacil.backend.cv.app.ExtractorIa.EducacionIa;
import com.camellofacil.backend.cv.app.ExtractorIa.ExperienciaIa;
import com.camellofacil.backend.cv.app.ExtractorIa.Extraccion;
import com.camellofacil.backend.legal.app.ConsentimientoService;
import com.camellofacil.backend.legal.app.TipoConsentimiento;
import com.camellofacil.backend.perfil.app.ModelosPerfil.DatosBasicos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.TipoPersona;
import com.camellofacil.backend.perfil.app.PerfilService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Subida, análisis en segundo plano (con IA falsa), revisión y confirmación de la hoja de vida. */
class HojaDeVidaIntegracionTest extends PruebaIntegracion {

    @Autowired
    UsuarioService usuarios;

    @Autowired
    PerfilService perfiles;

    @Autowired
    ConsentimientoService consentimientos;

    @Autowired
    AnalisisHojaService analisis;

    private UUID usuarioId;
    private RequestPostProcessor persona;

    @BeforeEach
    void personaConPerfilCompleto() {
        String oid = UUID.randomUUID().toString();
        UsuarioActual u = usuarios.registrarAcceso(UsuarioService.PROVEEDOR_MICROSOFT, oid, "hoja." + oid + "@unaula.edu.co", "Ana Prueba");
        usuarioId = u.id();
        perfiles.completar(usuarioId, new DatosBasicos(TipoPersona.ESTUDIANTE, "datos", 7));
        persona = oidcLogin().idToken(t -> t.claim("oid", oid));
        ExtractorIaDePrueba.RESPUESTA.set(new Extraccion(
                "es", "Ana Prueba Ficticia", "300 123 4567", "Medellin", "Perfil", "Python, SQL",
                List.of(new ExperienciaIa("Monitora de laboratorio", "Universidad de Ejemplo", "2024-02", "2024-11", null),
                        new ExperienciaIa("Cargo inventado", "Universidad de Ejemplo", "2023-01", null, null)),
                List.of(new EducacionIa("Ingenieria en Ciencia de Datos", "Universidad de Ejemplo", "2021-01", null))));
        // Las demás pruebas pueden dejar hojas en la cola: se vacía para que este caso tome la suya.
        while (analisis.procesarSiguiente()) {
            // vaciar cola
        }
    }

    private MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, "application/octet-stream", contenido);
    }

    @Test
    void recorridoCompleto_subirAnalizarRevisarYConfirmar() throws Exception {
        mockMvc.perform(get("/api/v1/hoja-de-vida").with(persona))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("sin_hoja"));

        mockMvc.perform(multipart("/api/v1/hoja-de-vida/archivo")
                        .file(archivo("CV Ana.pdf", DocumentosDePrueba.pdf(DocumentosDePrueba.LINEAS)))
                        .with(persona).with(csrf()))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/hoja-de-vida/analisis").with(persona))
                .andExpect(jsonPath("$.estado").value("analizando"));

        assertThat(analisis.procesarSiguiente()).isTrue();

        mockMvc.perform(get("/api/v1/hoja-de-vida/analisis").with(persona))
                .andExpect(jsonPath("$.estado").value("listo"));
        mockMvc.perform(get("/api/v1/hoja-de-vida").with(persona))
                .andExpect(jsonPath("$.estado").value("sin_confirmar"))
                .andExpect(jsonPath("$.hoja.archivo.nombre").value("CV Ana.pdf"))
                .andExpect(jsonPath("$.pendientesRevision").value(1));

        mockMvc.perform(get("/api/v1/hoja-de-vida/datos").with(persona))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contacto.nombre.valor").value("Ana Prueba Ficticia"))
                .andExpect(jsonPath("$.contacto.nombre.estado").value("leido"))
                // El correo es el de la cuenta, no el del archivo.
                .andExpect(jsonPath("$.contacto.correo.valor").value(org.hamcrest.Matchers.startsWith("hoja.")))
                .andExpect(jsonPath("$.experiencia[1].cargo.estado").value("revisa"))
                .andExpect(jsonPath("$.educacion[0].titulo.estado").value("leido"));

        mockMvc.perform(put("/api/v1/hoja-de-vida/datos").with(persona).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Prueba Ficticia","telefono":"300 123 4567","ciudad":"Medellín",
                                 "perfil":"Perfil","habilidades":"Python, SQL",
                                 "experiencia":[{"id":"e1","cargo":"Monitora","organizacion":"U","inicio":"2024-02","fin":"","descripcion":""}],
                                 "educacion":[]}"""))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/hoja-de-vida/datos/confirmacion").with(persona).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/hoja-de-vida").with(persona))
                .andExpect(jsonPath("$.estado").value("confirmada"))
                .andExpect(jsonPath("$.hoja.harvard").doesNotExist());
        mockMvc.perform(get("/api/v1/hoja-de-vida/datos").with(persona))
                .andExpect(jsonPath("$.experiencia.length()").value(1))
                .andExpect(jsonPath("$.experiencia[0].cargo.estado").value("leido"));
    }

    @Test
    void rechazaLoQueNoEsPdfNiWordAunqueDigaSerlo() throws Exception {
        mockMvc.perform(multipart("/api/v1/hoja-de-vida/archivo")
                        .file(archivo("hoja.pdf", "esto no es un pdf".getBytes(StandardCharsets.UTF_8)))
                        .with(persona).with(csrf()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo").value("tipo_no_permitido"));
    }

    @Test
    void sinAutorizacionDeIaNoSeRecibeElArchivo() throws Exception {
        consentimientos.revocar(usuarioId, TipoConsentimiento.ANALISIS_IA);
        mockMvc.perform(multipart("/api/v1/hoja-de-vida/archivo")
                        .file(archivo("CV.docx", DocumentosDePrueba.docx(DocumentosDePrueba.LINEAS)))
                        .with(persona).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("sin_autorizacion_ia"));
    }

    @Test
    void unEscaneoSinTextoQuedaIlegible() throws Exception {
        mockMvc.perform(multipart("/api/v1/hoja-de-vida/archivo")
                        .file(archivo("escaneo.pdf", DocumentosDePrueba.pdf("Hoja")))
                        .with(persona).with(csrf()))
                .andExpect(status().isAccepted());
        analisis.procesarSiguiente();
        mockMvc.perform(get("/api/v1/hoja-de-vida/analisis").with(persona))
                .andExpect(jsonPath("$.estado").value("ilegible"));
        mockMvc.perform(get("/api/v1/hoja-de-vida/datos").with(persona)).andExpect(status().isConflict());
    }

    @Test
    void siLaIaFallaSeReintentaYNoSePierdeLaHoja() throws Exception {
        ExtractorIaDePrueba.RESPUESTA.set(null);
        mockMvc.perform(multipart("/api/v1/hoja-de-vida/archivo")
                        .file(archivo("CV.docx", DocumentosDePrueba.docx(DocumentosDePrueba.LINEAS)))
                        .with(persona).with(csrf()))
                .andExpect(status().isAccepted());
        analisis.procesarSiguiente();
        // Sigue en cola para un reintento (no se marca error al primer fallo).
        mockMvc.perform(get("/api/v1/hoja-de-vida/analisis").with(persona))
                .andExpect(jsonPath("$.estado").value("analizando"));
    }

    @Test
    void sinSesionNoHayHoja() throws Exception {
        mockMvc.perform(get("/api/v1/hoja-de-vida")).andExpect(status().isUnauthorized());
    }
}
