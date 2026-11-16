package com.camellofacil.backend.vacantes.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** Una vacante que una persona guardó. */
@Entity
@Table(name = "vacante_marca")
public class MarcaVacante {

    @Embeddable
    public record Clave(
            @Column(name = "usuario_id") UUID usuarioId,
            @Column(name = "vacante_id") UUID vacanteId,
            @Column(length = 10) String tipo) implements Serializable {}

    @EmbeddedId
    private Clave id;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected MarcaVacante() {
        // Requerido por JPA
    }

    public MarcaVacante(Clave id, Instant creadaEn) {
        this.id = id;
        this.creadaEn = creadaEn;
    }

    public Clave getId() {
        return id;
    }
}
