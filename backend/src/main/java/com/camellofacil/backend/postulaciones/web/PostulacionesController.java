package com.camellofacil.backend.postulaciones.web;

import com.camellofacil.backend.auth.app.SesionActual;
import com.camellofacil.backend.postulaciones.app.ModelosPostulacion.ClicRegistrado;
import com.camellofacil.backend.postulaciones.app.ModelosPostulacion.HojaUsada;
import com.camellofacil.backend.postulaciones.app.ModelosPostulacion.ListaPostulaciones;
import com.camellofacil.backend.postulaciones.app.ModelosPostulacion.Orden;
import com.camellofacil.backend.postulaciones.app.ModelosPostulacion.PostulacionResumen;
import com.camellofacil.backend.postulaciones.app.PostulacionesService;
import com.camellofacil.backend.postulaciones.app.PostulacionesService.PostulacionNoEncontradaException;
import com.camellofacil.backend.vacantes.app.VacantesService.VacanteNoEncontradaException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * «Postular» y «Mis postulaciones». Postular registra el clic y devuelve la página original de la oferta,
 * donde la persona termina de postularse. Todo es del usuario de la sesión, nunca de uno que venga en la petición.
 */
@RestController
public class PostulacionesController {

    private static final Map<String, Orden> ORDENES = Map.of(
            "recientes", Orden.RECIENTES,
            "antiguas", Orden.ANTIGUAS,
            "cargoAZ", Orden.CARGO_AZ,
            "cargoZA", Orden.CARGO_ZA);

    private final SesionActual sesion;
    private final PostulacionesService postulaciones;

    public PostulacionesController(SesionActual sesion, PostulacionesService postulaciones) {
        this.sesion = sesion;
        this.postulaciones = postulaciones;
    }

    /** {@code hoja}: «base» (por omisión) o «adaptada». */
    public record ClicSolicitud(String hoja) {}

    public record ClicRespuesta(String urlOriginal, UUID postulacionId) {}

    public record PostulacionRespuesta(
            UUID id,
            UUID vacanteId,
            String cargo,
            String empresa,
            String ciudad,
            String modalidad,
            String tipo,
            Instant ultimoClicEn,
            int clics,
            String hojaUsada) {}

    public record ListaRespuesta(List<PostulacionRespuesta> items, long clicsUltimos90Dias) {}

    @PostMapping("/api/v1/vacantes/{vacanteId}/clics")
    public ClicRespuesta clicEnVacante(
            @PathVariable UUID vacanteId, @RequestBody(required = false) ClicSolicitud solicitud) {
        HojaUsada hoja = hoja(solicitud == null ? null : solicitud.hoja());
        return respuesta(postulaciones.clicEnVacante(sesion.requerido().id(), vacanteId, hoja));
    }

    @GetMapping("/api/v1/postulaciones")
    public ListaRespuesta listar(
            @RequestParam(name = "q", required = false) String texto,
            @RequestParam(name = "orden", defaultValue = "recientes") String orden) {
        Orden o = ORDENES.get(orden);
        if (o == null) throw new SolicitudInvalidaException("Orden no válido");
        ListaPostulaciones lista = postulaciones.listar(sesion.requerido().id(), texto, o);
        return new ListaRespuesta(
                lista.items().stream().map(PostulacionesController::respuesta).toList(), lista.clicsUltimos90Dias());
    }

    @PostMapping("/api/v1/postulaciones/{id}/clics")
    public ClicRespuesta clicDesdeHistorial(@PathVariable UUID id) {
        return respuesta(postulaciones.clicDesdeHistorial(sesion.requerido().id(), id));
    }

    @DeleteMapping("/api/v1/postulaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@PathVariable UUID id) {
        postulaciones.quitar(sesion.requerido().id(), id);
    }

    @PostMapping("/api/v1/postulaciones/{id}/restauracion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restaurar(@PathVariable UUID id) {
        postulaciones.restaurar(sesion.requerido().id(), id);
    }

    @ExceptionHandler(PostulacionNoEncontradaException.class)
    ProblemDetail postulacionNoEncontrada(PostulacionNoEncontradaException e) {
        return problema(HttpStatus.NOT_FOUND, e.getMessage(), "postulacion_no_encontrada");
    }

    @ExceptionHandler(VacanteNoEncontradaException.class)
    ProblemDetail vacanteNoEncontrada(VacanteNoEncontradaException e) {
        return problema(HttpStatus.NOT_FOUND, e.getMessage(), "vacante_no_encontrada");
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    ProblemDetail solicitudInvalida(SolicitudInvalidaException e) {
        return problema(HttpStatus.BAD_REQUEST, e.getMessage(), "solicitud_invalida");
    }

    private static HojaUsada hoja(String valor) {
        if (valor == null || valor.equals("base")) return HojaUsada.BASE;
        if (valor.equals("adaptada")) return HojaUsada.ADAPTADA;
        throw new SolicitudInvalidaException("Hoja no válida");
    }

    private static ProblemDetail problema(HttpStatus estado, String detalle, String codigo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setProperty("codigo", codigo);
        return problema;
    }

    private static ClicRespuesta respuesta(ClicRegistrado c) {
        return new ClicRespuesta(c.url(), c.postulacionId());
    }

    private static PostulacionRespuesta respuesta(PostulacionResumen p) {
        return new PostulacionRespuesta(
                p.id(),
                p.vacanteId(),
                p.cargo(),
                p.empresa(),
                p.ciudad(),
                p.modalidad().toLowerCase(Locale.ROOT),
                p.tipo().toLowerCase(Locale.ROOT),
                p.ultimoClicEn(),
                p.clics(),
                p.hojaUsada().name().toLowerCase(Locale.ROOT));
    }

    static class SolicitudInvalidaException extends RuntimeException {
        SolicitudInvalidaException(String mensaje) {
            super(mensaje);
        }
    }
}
