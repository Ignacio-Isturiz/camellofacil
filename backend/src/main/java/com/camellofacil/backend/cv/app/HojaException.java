package com.camellofacil.backend.cv.app;

/** Una regla de la hoja de vida no se cumple. La capa web lo traduce a un error para el usuario. */
public class HojaException extends RuntimeException {

    public enum Motivo {
        /** No es PDF ni Word, aunque la extensión lo diga. */
        TIPO_NO_PERMITIDO,
        /** Archivo vacío. */
        VACIO,
        /** La persona revocó la autorización de análisis con IA. */
        SIN_AUTORIZACION_IA,
        /** No hay hoja de vida (o aún no tiene datos). */
        SIN_HOJA,
        /** La operación no corresponde al estado actual (p. ej. confirmar mientras se analiza). */
        ESTADO_INVALIDO,
        /** Datos corregidos que no cumplen las reglas. */
        DATOS_INVALIDOS,
        /** Se llegó al tope diario de una operación con IA. */
        LIMITE,
        /** El proveedor de IA no respondió bien. */
        IA_NO_DISPONIBLE
    }

    private final Motivo motivo;

    public HojaException(Motivo motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
