package com.camellofacil.backend.perfil.app;

/** Una regla del perfil no se cumple. El código lo traduce la capa web a un error para el usuario. */
public class PerfilException extends RuntimeException {

    public enum Motivo {
        /** Datos que no cumplen las reglas (carrera inexistente, semestre que no corresponde...). */
        DATOS_INVALIDOS,
        /** Se intentó completar un perfil que ya está completo. */
        YA_COMPLETO,
        /** Se intentó editar antes de completar el primer ingreso. */
        INCOMPLETO
    }

    private final Motivo motivo;

    public PerfilException(Motivo motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
