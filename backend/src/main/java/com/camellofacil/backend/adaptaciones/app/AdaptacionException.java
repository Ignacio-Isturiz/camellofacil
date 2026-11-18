package com.camellofacil.backend.adaptaciones.app;

/** Una regla de la adaptación no se cumple. La capa web lo traduce a un error para el usuario. */
public class AdaptacionException extends RuntimeException {

    public enum Motivo {
        /** La vacante o la adaptación no existe o no es de la persona. */
        NO_ENCONTRADA,
        /** Se acabaron las adaptaciones de hoy. */
        LIMITE,
        /** La persona revocó la autorización de análisis con IA. */
        SIN_AUTORIZACION_IA,
        /** El proveedor de IA no respondió bien: no se gastó ninguna adaptación. */
        IA_NO_DISPONIBLE
    }

    private final Motivo motivo;

    public AdaptacionException(Motivo motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
