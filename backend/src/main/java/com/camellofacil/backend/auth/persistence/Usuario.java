package com.camellofacil.backend.auth.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persona que inicia sesión. Se identifica por el id estable del proveedor, no por el correo. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String proveedor;

    @Column(name = "id_externo", nullable = false, length = 100)
    private String idExterno;

    @Column(nullable = false, length = 320)
    private String correo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "ultimo_acceso", nullable = false)
    private Instant ultimoAcceso;

    protected Usuario() {
        // Requerido por JPA
    }

    public Usuario(String proveedor, String idExterno, String correo, String nombre, Instant ahora) {
        this.id = UUID.randomUUID();
        this.proveedor = proveedor;
        this.idExterno = idExterno;
        this.correo = correo;
        this.nombre = nombre;
        this.creadoEn = ahora;
        this.ultimoAcceso = ahora;
    }

    public void registrarAcceso(String correo, String nombre, Instant ahora) {
        this.correo = correo;
        this.nombre = nombre;
        this.ultimoAcceso = ahora;
    }

    public UUID getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public String getNombre() {
        return nombre;
    }
}
