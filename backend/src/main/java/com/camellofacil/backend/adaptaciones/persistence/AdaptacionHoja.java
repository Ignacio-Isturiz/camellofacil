package com.camellofacil.backend.adaptaciones.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Una adaptación de la hoja a una vacante. Los documentos y listas van en JSON; la capa app los interpreta. */
@Entity
@Table(name = "adaptacion_hoja")
public class AdaptacionHoja {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "vacante_id", nullable = false)
    private UUID vacanteId;

    @Column(name = "plantilla_id", nullable = false, length = 40)
    private String plantillaId;

    @Column(nullable = false, length = 2)
    private String idioma;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String valores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String original;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String adaptada;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String cambios;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String avisos;

    @Column(name = "version_id")
    private UUID versionId;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected AdaptacionHoja() {}

    public AdaptacionHoja(
            UUID usuarioId, UUID vacanteId, String plantillaId, String idioma, String valores, String original,
            String adaptada, String cambios, String avisos, Instant creadaEn) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.vacanteId = vacanteId;
        this.plantillaId = plantillaId;
        this.idioma = idioma;
        this.valores = valores;
        this.original = original;
        this.adaptada = adaptada;
        this.cambios = cambios;
        this.avisos = avisos;
        this.creadaEn = creadaEn;
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public UUID getVacanteId() { return vacanteId; }
    public String getPlantillaId() { return plantillaId; }
    public String getIdioma() { return idioma; }
    public String getValores() { return valores; }
    public String getOriginal() { return original; }
    public String getAdaptada() { return adaptada; }
    public String getCambios() { return cambios; }
    public String getAvisos() { return avisos; }
    public UUID getVersionId() { return versionId; }

    public void guardadaComoVersion(UUID version) { this.versionId = version; }
}
