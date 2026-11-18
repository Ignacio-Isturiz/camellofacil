package com.camellofacil.backend.adaptaciones.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/** Adaptaciones gastadas por una persona en un día. Solo se escribe con {@link UsoAdaptacionRepository#consumir}. */
@Entity
@Table(name = "uso_adaptacion")
public class UsoAdaptacion {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, insertable = false, updatable = false)
    private UUID usuarioId;

    @Column(nullable = false, insertable = false, updatable = false)
    private LocalDate fecha;

    @Column(nullable = false, insertable = false, updatable = false)
    private int usadas;

    protected UsoAdaptacion() {}

    public int getUsadas() { return usadas; }
}
