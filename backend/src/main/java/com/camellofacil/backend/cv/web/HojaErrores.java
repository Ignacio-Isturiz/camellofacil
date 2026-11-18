package com.camellofacil.backend.cv.web;

import com.camellofacil.backend.cv.app.HojaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las reglas de la hoja de vida a errores HTTP. Los códigos de estado son los que la interfaz usa
 * para elegir el mensaje (415 formato, 422 ilegible, 413 peso; este último lo resuelve Spring).
 */
@RestControllerAdvice(assignableTypes = HojaDeVidaController.class)
class HojaErrores {

    @ExceptionHandler(HojaException.class)
    ProblemDetail regla(HojaException e) {
        HttpStatus estado = switch (e.motivo()) {
            case TIPO_NO_PERMITIDO -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case VACIO -> HttpStatus.UNPROCESSABLE_CONTENT;
            case SIN_AUTORIZACION_IA -> HttpStatus.FORBIDDEN;
            case SIN_HOJA -> HttpStatus.NOT_FOUND;
            case ESTADO_INVALIDO -> HttpStatus.CONFLICT;
            case DATOS_INVALIDOS -> HttpStatus.BAD_REQUEST;
            case LIMITE -> HttpStatus.TOO_MANY_REQUESTS;
            case IA_NO_DISPONIBLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, e.getMessage());
        problema.setProperty("codigo", e.motivo().name().toLowerCase());
        return problema;
    }
}
