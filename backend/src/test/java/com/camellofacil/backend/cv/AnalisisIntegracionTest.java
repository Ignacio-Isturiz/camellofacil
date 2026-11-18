package com.camellofacil.backend.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camellofacil.backend.ExtractorIaDePrueba;
import com.camellofacil.backend.PruebaIntegracion;
import com.camellofacil.backend.RecomendadorIaDePrueba;
import com.camellofacil.backend.auth.app.UsuarioActual;
import com.camellofacil.backend.auth.app.UsuarioService;
import com.camellofacil.backend.cv.app.AnalisisHojaService;
import com.camellofacil.backend.cv.app.ExtractorIa.Extraccion;
import com.camellofacil.backend.cv.app.HojaDeVidaService;
import com.camellofacil.backend.cv.app.ModelosAnalisis.RecomendacionIa;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Análisis Harvard / ATS de la hoja confirmada: reglas siempre, recomendaciones de la IA a pedido y guardadas. */
class AnalisisIntegracionTest extends PruebaIntegracion {

    @Autowired UsuarioService usuarios;
    @Autowired PerfilService perfiles;
    @Autowired ConsentimientoService consentimientos;
    @Autowired HojaDeVidaService hojas;
    @Autowired AnalisisHojaService analisis;

    private record Persona(UUID id, RequestPostProcessor sesion) {}

    @BeforeEach
    void preparar() {
        RecomendadorIaDePrueba.LLAMADAS.set(0);
        RecomendadorIaDePrueba.ULTIMA_HOJA.set(null);
        RecomendadorIaDePrueba.RESPUESTA.set(List.of(
                new RecomendacionIa("Agrega cifras a tus logros", "Si las tienes, suma porcentajes o tiempos."),
                new RecomendacionIa("Enlace sospechoso", "Visita https://malicioso.example ahora")));
    }

    private ValoresHoja valores(String perfil) {
        return new ValoresHoja("Ana Prueba Ficticia", "300 123 4567", "Medellín", perfil, "Python, SQL, Excel",
                List.of(new ValorExperiencia("e1", "Monitora", "Universidad de Ejemplo", "2024-02", "2024-11", "Apoyé prácticas de laboratorio.")),
                List.of(new ValorEducacion("d1", "Ingeniería de Sistemas", "Universidad de Ejemplo", "2021-01", "")));
    }

    private Persona persona(boolean confirmar) throws Exception {
        String oid = UUID.randomUUID().toString();
        UsuarioActual u = usuarios.registrarAcceso(UsuarioService.PROVEEDOR_MICROSOFT, oid, "ana." + oid + "@unaula.edu.co", "Ana Prueba");
        perfiles.completar(u.id(), new DatosBasicos(TipoPersona.ESTUDIANTE, "datos", 7));
        ExtractorIaDePrueba.RESPUESTA.set(new Extraccion("es", "Ana Prueba Ficticia", "300 123 4567", "Medellin", "Perfil", "Python", List.of(), List.of()));
        while (analisis.procesarSiguiente()) {
            // vaciar la cola de otras pruebas
        }
        hojas.subir(u.id(), "cv.pdf", DocumentosDePrueba.pdf(DocumentosDePrueba.LINEAS));
        assertThat(analisis.procesarSiguiente()).isTrue();
        if (confirmar) editarYConfirmar(u.id(), "Estudiante de datos enfocada en análisis.");
        return new Persona(u.id(), oidcLogin().idToken(t -> t.claim("oid", oid)));
    }

    /** Cada cambio de la hoja la deja «nueva»: las recomendaciones guardadas dejan de valer. */
    private void editarYConfirmar(UUID id, String perfil) throws Exception {
        Thread.sleep(5); // que la marca de tiempo avance
        hojas.corregir(id, valores(perfil));
        hojas.confirmar(id);
    }

    @Test
    void sinConfirmarNoHayAnalisis_conConfirmarLlegaPuntajeYObservacionesPorReglas() throws Exception {
        Persona sinConfirmar = persona(false);
        mockMvc.perform(get("/api/v1/hoja-de-vida").with(sinConfirmar.sesion()))
                .andExpect(jsonPath("$.estado").value("sin_confirmar"))
                .andExpect(jsonPath("$.hoja.harvard").doesNotExist())
                .andExpect(jsonPath("$.hoja.observaciones.length()").value(0));

        Persona p = persona(true);
        mockMvc.perform(get("/api/v1/hoja-de-vida").with(p.sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("confirmada"))
                .andExpect(jsonPath("$.hoja.harvard.valor").isNumber())
                .andExpect(jsonPath("$.hoja.harvard.nivel").isString())
                .andExpect(jsonPath("$.hoja.ats.opcionId").value("general"))
                .andExpect(jsonPath("$.hoja.opcionesAts.length()").value(3))
                .andExpect(jsonPath("$.hoja.observaciones[?(@.id=='h-perfil')].tipo").value("bien"))
                .andExpect(jsonPath("$.hoja.observaciones[?(@.origen=='ia')]").isEmpty())
                .andExpect(jsonPath("$.hoja.recomendacionesIa").value("pendiente"));
        // Mirar el análisis no llama a la IA.
        assertThat(RecomendadorIaDePrueba.LLAMADAS.get()).isZero();
    }

    @Test
    void otraOpcionAtsDaOtroPuntaje_yUnaInexistenteDa404() throws Exception {
        Persona p = persona(true);
        mockMvc.perform(get("/api/v1/hoja-de-vida/puntajes/ats/palabras-clave").with(p.sesion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opcionId").value("palabras-clave"))
                .andExpect(jsonPath("$.valor").isNumber());
        mockMvc.perform(get("/api/v1/hoja-de-vida/puntajes/ats/no-existe").with(p.sesion())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/hoja-de-vida/puntajes/ats/general")).andExpect(status().isUnauthorized());
    }

    @Test
    void lasRecomendacionesDeLaIaSeGuardan_seLimpian_ynoSeRepitenSiLaHojaNoCambia() throws Exception {
        Persona p = persona(true);

        mockMvc.perform(post("/api/v1/hoja-de-vida/recomendaciones-ia").with(p.sesion()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoja.recomendacionesIa").value("lista"))
                .andExpect(jsonPath("$.hoja.observaciones[?(@.origen=='ia')].titulo").value("Agrega cifras a tus logros"))
                // La recomendación con enlace se descarta.
                .andExpect(jsonPath("$.hoja.observaciones[?(@.titulo=='Enlace sospechoso')]").isEmpty());
        // La IA no recibe datos de contacto.
        assertThat(RecomendadorIaDePrueba.ULTIMA_HOJA.get().nombre()).isEmpty();
        assertThat(RecomendadorIaDePrueba.ULTIMA_HOJA.get().telefono()).isEmpty();

        // Se ven al volver a la pantalla y pedirlas otra vez no gasta otra llamada.
        mockMvc.perform(get("/api/v1/hoja-de-vida").with(p.sesion()))
                .andExpect(jsonPath("$.hoja.recomendacionesIa").value("lista"))
                .andExpect(jsonPath("$.hoja.observaciones[?(@.origen=='ia')].tipo").value("mejorar"));
        mockMvc.perform(post("/api/v1/hoja-de-vida/recomendaciones-ia").with(p.sesion()).with(csrf())).andExpect(status().isOk());
        assertThat(RecomendadorIaDePrueba.LLAMADAS.get()).isEqualTo(1);

        // Si la hoja cambia, hay que pedirlas de nuevo.
        editarYConfirmar(p.id(), "Perfil nuevo, corregido por la persona.");
        mockMvc.perform(get("/api/v1/hoja-de-vida").with(p.sesion()))
                .andExpect(jsonPath("$.hoja.recomendacionesIa").value("pendiente"))
                .andExpect(jsonPath("$.hoja.observaciones[?(@.origen=='ia')]").isEmpty());
    }

    @Test
    void siLaIaFallaNoSeGastaElTopeNiSeGuardaNada() throws Exception {
        Persona p = persona(true);
        RecomendadorIaDePrueba.RESPUESTA.set(null);

        mockMvc.perform(post("/api/v1/hoja-de-vida/recomendaciones-ia").with(p.sesion()).with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("ia_no_disponible"));
        mockMvc.perform(get("/api/v1/hoja-de-vida").with(p.sesion()))
                .andExpect(jsonPath("$.hoja.recomendacionesIa").value("pendiente"));

        RecomendadorIaDePrueba.RESPUESTA.set(List.of(new RecomendacionIa("Una", "Sí funciona después")));
        mockMvc.perform(post("/api/v1/hoja-de-vida/recomendaciones-ia").with(p.sesion()).with(csrf())).andExpect(status().isOk());
    }

    @Test
    void sinAutorizacionDeIaNoHayRecomendacionesDeIa_peroLasReglasSiguen() throws Exception {
        Persona p = persona(true);
        consentimientos.revocar(p.id(), TipoConsentimiento.ANALISIS_IA);

        mockMvc.perform(get("/api/v1/hoja-de-vida").with(p.sesion()))
                .andExpect(jsonPath("$.hoja.recomendacionesIa").value("no_disponible"))
                .andExpect(jsonPath("$.hoja.harvard.valor").isNumber());
        mockMvc.perform(post("/api/v1/hoja-de-vida/recomendaciones-ia").with(p.sesion()).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("sin_autorizacion_ia"));
        assertThat(RecomendadorIaDePrueba.LLAMADAS.get()).isZero();
    }

    @Test
    void despuesDeCincoGeneracionesEnUnDiaSeAcabaElTope() throws Exception {
        Persona p = persona(true);
        for (int i = 0; i < 5; i++) {
            editarYConfirmar(p.id(), "Perfil versión " + i + " de la hoja.");
            mockMvc.perform(post("/api/v1/hoja-de-vida/recomendaciones-ia").with(p.sesion()).with(csrf())).andExpect(status().isOk());
        }
        editarYConfirmar(p.id(), "Perfil versión sexta de la hoja.");
        mockMvc.perform(post("/api/v1/hoja-de-vida/recomendaciones-ia").with(p.sesion()).with(csrf()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("limite"));
        assertThat(RecomendadorIaDePrueba.LLAMADAS.get()).isEqualTo(5);
    }

    @Test
    void sinHojaConfirmadaNoSePuedenPedirRecomendaciones() throws Exception {
        Persona p = persona(false);
        mockMvc.perform(post("/api/v1/hoja-de-vida/recomendaciones-ia").with(p.sesion()).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/hoja-de-vida/recomendaciones-ia")).andExpect(status().isForbidden());
    }
}
