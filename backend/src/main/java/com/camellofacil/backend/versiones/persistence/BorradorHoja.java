package com.camellofacil.backend.versiones.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Borrador del editor: los valores en que va la persona, antes de guardarlos como versión. */
@Entity
@Table(name = "borrador_hoja")
public class BorradorHoja {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "plantilla_id", nullable = false, length = 40)
    private String plantillaId;

    @Column(nullable = false, length = 2)
    private String idioma;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String valores;

    @Column(name = "guardado_en")
    private Instant guardadoEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected BorradorHoja() {}

    public BorradorHoja(UUID usuarioId, String plantillaId, String idioma, String valores, Instant creadoEn) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.plantillaId = plantillaId;
        this.idioma = idioma;
        this.valores = valores;
        this.creadoEn = creadoEn;
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getPlantillaId() { return plantillaId; }
    public String getIdioma() { return idioma; }
    public String getValores() { return valores; }
    public Instant getGuardadoEn() { return guardadoEn; }

    public void guardar(String nuevosValores, Instant cuando) {
        this.valores = nuevosValores;
        this.guardadoEn = cuando;
    }
}
