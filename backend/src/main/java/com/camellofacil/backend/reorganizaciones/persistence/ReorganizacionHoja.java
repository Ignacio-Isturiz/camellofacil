package com.camellofacil.backend.reorganizaciones.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Una propuesta de reorganización de la hoja. Los datos (hoja, redacción, plan) van en JSON; la capa app los interpreta. */
@Entity
@Table(name = "reorganizacion_hoja")
public class ReorganizacionHoja {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 10)
    private String formato;

    @Column(nullable = false, length = 2)
    private String idioma;

    @Column(name = "base_huella", nullable = false, length = 64)
    private String baseHuella;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String base;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String ia;

    @Column(name = "ia_generada", nullable = false)
    private boolean iaGenerada;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String plan;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String avisos;

    @Column(name = "version_id")
    private UUID versionId;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected ReorganizacionHoja() {}

    public ReorganizacionHoja(
            UUID usuarioId, String formato, String idioma, String baseHuella, String base, String ia, boolean iaGenerada,
            String plan, String avisos, Instant creadaEn) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.formato = formato;
        this.idioma = idioma;
        this.baseHuella = baseHuella;
        this.base = base;
        this.ia = ia;
        this.iaGenerada = iaGenerada;
        this.plan = plan;
        this.avisos = avisos;
        this.creadaEn = creadaEn;
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getFormato() { return formato; }
    public String getIdioma() { return idioma; }
    public String getBaseHuella() { return baseHuella; }
    public String getBase() { return base; }
    public String getIa() { return ia; }
    public boolean isIaGenerada() { return iaGenerada; }
    public String getPlan() { return plan; }
    public String getAvisos() { return avisos; }
    public UUID getVersionId() { return versionId; }

    public void nuevoPlan(String plan) { this.plan = plan; }

    public void guardadaComoVersion(UUID version) { this.versionId = version; }
}
