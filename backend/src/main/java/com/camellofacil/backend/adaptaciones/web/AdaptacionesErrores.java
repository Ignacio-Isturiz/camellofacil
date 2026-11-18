package com.camellofacil.backend.adaptaciones.web;

import com.camellofacil.backend.adaptaciones.app.AdaptacionException;
import com.camellofacil.backend.cv.app.HojaException;
import com.camellofacil.backend.versiones.app.VersionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las reglas de la adaptación a errores HTTP. El 429 es el que la interfaz usa para «se acabó el cupo». */
@RestControllerAdvice(assignableTypes = AdaptacionesController.class)
class AdaptacionesErrores {

    @ExceptionHandler(AdaptacionException.class)
    ProblemDetail regla(AdaptacionException e) {
        HttpStatus estado = switch (e.motivo()) {
            case NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case LIMITE -> HttpStatus.TOO_MANY_REQUESTS;
            case SIN_AUTORIZACION_IA -> HttpStatus.FORBIDDEN;
            case IA_NO_DISPONIBLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return problema(estado, e.getMessage(), e.motivo().name().toLowerCase());
    }

    /** Sin hoja confirmada no hay nada que adaptar. */
    @ExceptionHandler(HojaException.class)
    ProblemDetail hoja(HojaException e) {
        HttpStatus estado = e.motivo() == HojaException.Motivo.SIN_HOJA ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        return problema(estado, e.getMessage(), e.motivo().name().toLowerCase());
    }

    /** Al guardar como versión: puede haber llegado al máximo de versiones. */
    @ExceptionHandler(VersionException.class)
    ProblemDetail version(VersionException e) {
        HttpStatus estado = e.motivo() == VersionException.Motivo.NO_ENCONTRADA ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        return problema(estado, e.getMessage(), e.motivo().name().toLowerCase());
    }

    private static ProblemDetail problema(HttpStatus estado, String detalle, String codigo) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(estado, detalle);
        p.setProperty("codigo", codigo);
        return p;
    }
}
