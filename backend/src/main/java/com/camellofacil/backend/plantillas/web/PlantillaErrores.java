package com.camellofacil.backend.plantillas.web;

import com.camellofacil.backend.cv.app.HojaException;
import com.camellofacil.backend.plantillas.app.PlantillaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PlantillasController.class)
class PlantillaErrores {

    @ExceptionHandler(PlantillaException.class)
    ProblemDetail noDisponible(PlantillaException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Sin hoja confirmada no hay datos con los que armar la vista previa. */
    @ExceptionHandler(HojaException.class)
    ProblemDetail sinHoja(HojaException e) {
        HttpStatus estado = e.motivo() == HojaException.Motivo.SIN_HOJA ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, e.getMessage());
        problema.setProperty("codigo", e.motivo().name().toLowerCase());
        return problema;
    }
}
