package com.camellofacil.backend.versiones.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Una versión guardada de la hoja de vida. Los valores de dominio (tipo, documento) los maneja la capa app. */
@Entity
@Table(name = "version_hoja")
public class VersionHoja {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 15)
    private String tipo;

    @Column(nullable = false, length = 2)
    private String idioma;

    @Column(name = "plantilla_id", nullable = false, length = 40)
    private String plantillaId;

    @Column(name = "es_base", nullable = false)
    private boolean esBase;

    @Column(length = 255)
    private String nota;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String documento;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String valores;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected VersionHoja() {}

    public VersionHoja(
            UUID usuarioId, String nombre, String tipo, String idioma, String plantillaId, boolean esBase, String nota,
            String documento, String valores, Instant creadaEn) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.idioma = idioma;
        this.plantillaId = plantillaId;
        this.esBase = esBase;
        this.nota = nota;
        this.documento = documento;
        this.valores = valores;
        this.creadaEn = creadaEn;
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public String getIdioma() { return idioma; }
    public String getPlantillaId() { return plantillaId; }
    public boolean isEsBase() { return esBase; }
    public String getNota() { return nota; }
    public String getDocumento() { return documento; }
    public String getValores() { return valores; }
    public Instant getCreadaEn() { return creadaEn; }

    public void renombrar(String nuevo) { this.nombre = nuevo; }

    public void marcarBase(boolean base) { this.esBase = base; }
}
