package com.camellofacil.backend.perfil.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Perfil de una persona. Los valores de dominio (tipo, modalidades, disponibilidad) los valida la capa app. */
@Entity
@Table(name = "perfil")
public class Perfil {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(name = "carrera_codigo", nullable = false, length = 40)
    private String carreraCodigo;

    private Short semestre;

    /** Lista separada por comas, p. ej. "PRESENCIAL,HIBRIDO". */
    @Column(length = 40)
    private String modalidades;

    @Column(length = 20)
    private String disponibilidad;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Perfil() {
        // Requerido por JPA
    }

    public Perfil(UUID usuarioId, Instant ahora) {
        this.usuarioId = usuarioId;
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    public void datosBasicos(String tipo, String carreraCodigo, Short semestre, Instant ahora) {
        this.tipo = tipo;
        this.carreraCodigo = carreraCodigo;
        this.semestre = semestre;
        this.actualizadoEn = ahora;
    }

    public void preferencias(String modalidades, String disponibilidad) {
        this.modalidades = modalidades;
        this.disponibilidad = disponibilidad;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCarreraCodigo() {
        return carreraCodigo;
    }

    public Short getSemestre() {
        return semestre;
    }

    public String getModalidades() {
        return modalidades;
    }

    public String getDisponibilidad() {
        return disponibilidad;
    }
}
