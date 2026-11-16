package com.camellofacil.backend.vacantes.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Una búsqueda que se lanza contra una fuente, con su prioridad (peso). */
@Entity
@Table(name = "consulta_vacantes")
public class ConsultaVacantes {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String fuente;

    @Column(name = "carrera_codigo", nullable = false, length = 30)
    private String carreraCodigo;

    @Column(nullable = false, length = 200)
    private String texto;

    @Column(nullable = false)
    private short peso;

    @Column(nullable = false)
    private boolean activa;

    @Column(name = "ultima_ejecucion")
    private Instant ultimaEjecucion;

    protected ConsultaVacantes() {
        // Requerido por JPA
    }

    public ConsultaVacantes(UUID id, String fuente, String carreraCodigo, String texto, short peso) {
        this.id = id;
        this.fuente = fuente;
        this.carreraCodigo = carreraCodigo;
        this.texto = texto;
        this.peso = peso;
        this.activa = true;
    }

    public void ejecutada(Instant cuando) {
        this.ultimaEjecucion = cuando;
    }

    public UUID getId() {
        return id;
    }

    public String getFuente() {
        return fuente;
    }

    public String getCarreraCodigo() {
        return carreraCodigo;
    }

    public String getTexto() {
        return texto;
    }

    public short getPeso() {
        return peso;
    }

    public Instant getUltimaEjecucion() {
        return ultimaEjecucion;
    }
}
