package com.camellofacil.backend.postulaciones.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Un clic en «Postular». Solo se usa para contar los clics de los últimos 90 días. */
@Entity
@Table(name = "clic_postulacion")
public class ClicPostulacion {

    @Id
    private UUID id;

    @Column(name = "postulacion_id", nullable = false)
    private UUID postulacionId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected ClicPostulacion() {
        // Requerido por JPA
    }

    public ClicPostulacion(UUID id, UUID postulacionId, Instant creadoEn) {
        this.id = id;
        this.postulacionId = postulacionId;
        this.creadoEn = creadoEn;
    }
}
