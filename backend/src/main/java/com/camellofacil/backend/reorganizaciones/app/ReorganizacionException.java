package com.camellofacil.backend.reorganizaciones.app;

/** Una regla de la reorganización no se cumple. La capa web lo traduce a un error para el usuario. */
public class ReorganizacionException extends RuntimeException {

    public enum Motivo {
        /** La propuesta o el cambio no existe o no es de la persona. */
        NO_ENCONTRADA,
        /** Formato o idioma que no existen. */
        DATOS_INVALIDOS
    }

    private final Motivo motivo;

    public ReorganizacionException(Motivo motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
