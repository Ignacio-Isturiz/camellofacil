package com.camellofacil.backend.postulaciones.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Una vacante en la que la persona dio clic en «Postular». Se crea y se suman clics con
 * {@link PostulacionRepository#registrarClic}; aquí solo se marca o desmarca como quitada.
 */
@Entity
@Table(name = "postulacion")
public class Postulacion {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "vacante_id", nullable = false)
    private UUID vacanteId;

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

    @Column(name = "hoja_usada", nullable = false, length = 10)
    private String hojaUsada;

    @Column(nullable = false)
    private int clics;

    @Column(name = "primer_clic_en", nullable = false)
    private Instant primerClicEn;

    @Column(name = "ultimo_clic_en", nullable = false)
    private Instant ultimoClicEn;

    @Column(name = "quitada_en")
    private Instant quitadaEn;

    protected Postulacion() {
        // Requerido por JPA
    }

    public void quitar(Instant ahora) {
        if (quitadaEn == null) quitadaEn = ahora;
    }

    public void restaurar() {
        quitadaEn = null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getVacanteId() {
        return vacanteId;
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

    public String getHojaUsada() {
        return hojaUsada;
    }

    public int getClics() {
        return clics;
    }

    public Instant getUltimoClicEn() {
        return ultimoClicEn;
    }

    public Instant getQuitadaEn() {
        return quitadaEn;
    }
}
