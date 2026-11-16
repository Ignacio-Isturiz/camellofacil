package com.camellofacil.backend.auth.app;

/** La petición no tiene una persona con cuenta. La capa web lo traduce a 401 y el frontend lleva a /login. */
public class SinSesionException extends RuntimeException {

    public SinSesionException() {
        super("Sin sesión");
    }
}
