package com.camellofacil.backend.cv.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Bytes del archivo original. Separado de {@link HojaDeVida} para no cargarlos al consultar el estado. */
@Entity
@Table(name = "hoja_de_vida_archivo")
public class ArchivoHoja {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false)
    private byte[] contenido;

    protected ArchivoHoja() {
        // Requerido por JPA
    }

    public ArchivoHoja(UUID usuarioId, byte[] contenido) {
        this.usuarioId = usuarioId;
        this.contenido = contenido;
    }

    public void reemplazar(byte[] contenido) {
        this.contenido = contenido;
    }

    public byte[] getContenido() {
        return contenido;
    }
}
