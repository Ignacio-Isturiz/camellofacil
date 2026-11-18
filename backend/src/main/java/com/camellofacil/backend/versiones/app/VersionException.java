package com.camellofacil.backend.versiones.app;

/** Una regla de las versiones o del editor no se cumple. La capa web lo traduce a un error para el usuario. */
public class VersionException extends RuntimeException {

    public enum Motivo {
        /** No existe o no es de la persona (el mensaje no distingue ambos casos). */
        NO_ENCONTRADA,
        /** La versión base no se elimina: primero hay que elegir otra como base. */
        NO_ELIMINABLE,
        /** Tiene demasiadas versiones guardadas. */
        LIMITE,
        /** Datos que no cumplen las reglas (nombre vacío, fechas mal escritas, falta la plantilla…). */
        DATOS_INVALIDOS
    }

    private final Motivo motivo;

    public VersionException(Motivo motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
