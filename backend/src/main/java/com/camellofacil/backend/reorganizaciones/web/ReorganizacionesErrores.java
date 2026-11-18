package com.camellofacil.backend.reorganizaciones.web;

import com.camellofacil.backend.cv.app.HojaException;
import com.camellofacil.backend.plantillas.app.PlantillaException;
import com.camellofacil.backend.reorganizaciones.app.ReorganizacionException;
import com.camellofacil.backend.versiones.app.VersionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las reglas de la reorganización a errores HTTP con mensajes en español claro. */
@RestControllerAdvice(assignableTypes = ReorganizacionesController.class)
class ReorganizacionesErrores {

    @ExceptionHandler(ReorganizacionException.class)
    ProblemDetail regla(ReorganizacionException e) {
        HttpStatus estado = e.motivo() == ReorganizacionException.Motivo.NO_ENCONTRADA ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return problema(estado, e.getMessage(), e.motivo().name().toLowerCase());
    }

    /** Sin hoja confirmada no hay nada que reorganizar. */
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

    @ExceptionHandler(PlantillaException.class)
    ProblemDetail plantilla(PlantillaException e) {
        return problema(HttpStatus.NOT_FOUND, e.getMessage(), "plantilla_no_encontrada");
    }

    private static ProblemDetail problema(HttpStatus estado, String detalle, String codigo) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(estado, detalle);
        p.setProperty("codigo", codigo);
        return p;
    }
}
