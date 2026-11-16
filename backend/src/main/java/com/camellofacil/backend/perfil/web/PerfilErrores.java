package com.camellofacil.backend.perfil.web;

import com.camellofacil.backend.perfil.app.PerfilException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las reglas del perfil a errores con el formato único (ProblemDetail) y mensajes en español. */
@RestControllerAdvice(assignableTypes = PerfilController.class)
class PerfilErrores {

    @ExceptionHandler(PerfilException.class)
    ProblemDetail regla(PerfilException e) {
        HttpStatus estado = switch (e.motivo()) {
            case DATOS_INVALIDOS -> HttpStatus.BAD_REQUEST;
            case YA_COMPLETO, INCOMPLETO -> HttpStatus.CONFLICT;
        };
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, e.getMessage());
        problema.setProperty("codigo", e.motivo().name().toLowerCase());
        return problema;
    }
}
