package com.camellofacil.backend.vacantes.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Vacante guardada. Se escribe solo con {@link VacanteRepository#guardarOActualizar}; aquí solo se lee. */
@Entity
@Table(name = "vacante")
public class Vacante {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String fuente;

    @Column(nullable = false, length = 300)
    private String cargo;

    @Column(nullable = false, length = 200)
    private String empresa;

    @Column(length = 120)
    private String ciudad;

    @Column(nullable = false, length = 10)
    private String modalidad;

    @Column(nullable = false, length = 12)
    private String tipo;

    @Column(length = 120)
    private String publicador;

    @Column(name = "url_postulacion", nullable = false, length = 2000)
    private String urlPostulacion;

    @Column(columnDefinition = "text")
    private String descripcion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String responsabilidades;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String requisitos;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private String[] carreras;

    @Column(name = "publicada_en", nullable = false)
    private Instant publicadaEn;

    @Column(name = "vista_en", nullable = false)
    private Instant vistaEn;

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    protected Vacante() {
        // Requerido por JPA
    }

    public UUID getId() {
        return id;
    }

    public String getFuente() {
        return fuente;
    }

    public String getCargo() {
        return cargo;
    }

    public String getEmpresa() {
        return empresa;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getModalidad() {
        return modalidad;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPublicador() {
        return publicador;
    }

    public String getUrlPostulacion() {
        return urlPostulacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getResponsabilidades() {
        return responsabilidades;
    }

    public String getRequisitos() {
        return requisitos;
    }

    public String[] getCarreras() {
        return carreras;
    }

    public Instant getPublicadaEn() {
        return publicadaEn;
    }

    public Instant getCerradaEn() {
        return cerradaEn;
    }
}
