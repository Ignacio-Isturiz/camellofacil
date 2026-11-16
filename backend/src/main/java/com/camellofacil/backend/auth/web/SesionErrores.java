package com.camellofacil.backend.auth.web;

import com.camellofacil.backend.auth.app.SinSesionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** En cualquier controlador, pedir la sesión sin tenerla responde 401 con el formato único de errores. */
@RestControllerAdvice
class SesionErrores {

    @ExceptionHandler(SinSesionException.class)
    ProblemDetail sinSesion() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
    }
}
