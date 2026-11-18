package com.camellofacil.backend.versiones.web;

import com.camellofacil.backend.cv.app.HojaException;
import com.camellofacil.backend.plantillas.app.PlantillaException;
import com.camellofacil.backend.versiones.app.VersionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las reglas de versiones y borradores a errores HTTP con mensajes en español claro. */
@RestControllerAdvice(assignableTypes = VersionesController.class)
class VersionesErrores {

    @ExceptionHandler(VersionException.class)
    ProblemDetail regla(VersionException e) {
        HttpStatus estado = switch (e.motivo()) {
            case NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case NO_ELIMINABLE, LIMITE -> HttpStatus.CONFLICT;
            case DATOS_INVALIDOS -> HttpStatus.BAD_REQUEST;
        };
        return problema(estado, e.getMessage(), e.motivo().name().toLowerCase());
    }

    @ExceptionHandler(PlantillaException.class)
    ProblemDetail plantilla(PlantillaException e) {
        return problema(HttpStatus.NOT_FOUND, e.getMessage(), "plantilla_no_encontrada");
    }

    /** Sin hoja confirmada no hay datos con los que armar versiones ni borradores. */
    @ExceptionHandler(HojaException.class)
    ProblemDetail hoja(HojaException e) {
        HttpStatus estado = e.motivo() == HojaException.Motivo.SIN_HOJA ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        return problema(estado, e.getMessage(), e.motivo().name().toLowerCase());
    }

    private static ProblemDetail problema(HttpStatus estado, String detalle, String codigo) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(estado, detalle);
        p.setProperty("codigo", codigo);
        return p;
    }
}
