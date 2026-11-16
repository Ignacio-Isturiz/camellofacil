package com.camellofacil.backend.perfil.web;

import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.auth.app.UsuarioActual;
import com.camellofacil.backend.legal.app.ConsentimientoService;
import com.camellofacil.backend.legal.app.ConsentimientoVigente;
import com.camellofacil.backend.legal.app.TipoConsentimiento;
import com.camellofacil.backend.perfil.app.ModelosPerfil.CarreraDatos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.DatosBasicos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.PerfilDatos;
import com.camellofacil.backend.perfil.app.ModelosPerfil.Preferencias;
import com.camellofacil.backend.perfil.app.PerfilException;
import com.camellofacil.backend.perfil.app.PerfilService;
import com.camellofacil.backend.perfil.web.PerfilDtos.ActualizarSolicitud;
import com.camellofacil.backend.perfil.web.PerfilDtos.CompletarSolicitud;
import com.camellofacil.backend.perfil.web.PerfilDtos.ConsentimientoRespuesta;
import com.camellofacil.backend.perfil.web.PerfilDtos.DatosRespuesta;
import com.camellofacil.backend.perfil.web.PerfilDtos.PerfilRespuesta;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/perfil")
public class PerfilController {

    private final SesionActual sesion;
    private final PerfilService perfiles;
    private final ConsentimientoService consentimientos;

    public PerfilController(SesionActual sesion, PerfilService perfiles, ConsentimientoService consentimientos) {
        this.sesion = sesion;
        this.perfiles = perfiles;
        this.consentimientos = consentimientos;
    }

    @GetMapping
    public PerfilRespuesta obtener() {
        return respuesta(sesion.requerido());
    }

    @GetMapping("/carreras")
    public List<CarreraDatos> carreras() {
        return perfiles.carreras();
    }

    /** Modal obligatorio del primer ingreso. */
    @PostMapping
    public PerfilRespuesta completar(@Valid @RequestBody CompletarSolicitud solicitud) {
        UsuarioActual usuario = sesion.requerido();
        // La persona debe aceptar exactamente la versión que se publica ahora, no una que vio antes.
        if (!ConsentimientoService.VERSION_TERMINOS.equals(solicitud.versionTerminos())) {
            throw new PerfilException(PerfilException.Motivo.DATOS_INVALIDOS, "Los términos cambiaron");
        }
        perfiles.completar(usuario.id(), new DatosBasicos(solicitud.tipo(), solicitud.carrera(), solicitud.semestre()));
        return respuesta(usuario);
    }

    @PutMapping
    public PerfilRespuesta actualizar(@Valid @RequestBody ActualizarSolicitud solicitud) {
        UsuarioActual usuario = sesion.requerido();
        perfiles.actualizar(
                usuario.id(),
                new DatosBasicos(solicitud.tipo(), solicitud.carrera(), solicitud.semestre()),
                new Preferencias(solicitud.modalidades(), solicitud.disponibilidad()));
        return respuesta(usuario);
    }

    private PerfilRespuesta respuesta(UsuarioActual usuario) {
        UUID id = usuario.id();
        DatosRespuesta datos = perfiles.obtener(id).map(PerfilController::aDatos).orElse(null);
        return new PerfilRespuesta(
                perfiles.estaCompleto(id),
                usuario.nombre(),
                usuario.correo(),
                ConsentimientoService.VERSION_TERMINOS,
                datos,
                consentimiento(id, TipoConsentimiento.TERMINOS_PRIVACIDAD),
                consentimiento(id, TipoConsentimiento.ANALISIS_IA));
    }

    private ConsentimientoRespuesta consentimiento(UUID usuarioId, TipoConsentimiento tipo) {
        return consentimientos.vigente(usuarioId, tipo).map(PerfilController::aConsentimiento).orElse(null);
    }

    private static ConsentimientoRespuesta aConsentimiento(ConsentimientoVigente c) {
        return new ConsentimientoRespuesta(c.version(), c.otorgadoEn());
    }

    private static DatosRespuesta aDatos(PerfilDatos p) {
        Preferencias pr = p.preferencias();
        return new DatosRespuesta(p.tipo(), p.carrera(), p.semestre(), pr.modalidades(), pr.disponibilidad());
    }
}
