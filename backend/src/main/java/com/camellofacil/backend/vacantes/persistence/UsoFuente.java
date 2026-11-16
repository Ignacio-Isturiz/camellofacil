package com.camellofacil.backend.vacantes.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Cuántas peticiones lleva una fuente en su periodo actual. */
@Entity
@Table(name = "uso_fuente")
public class UsoFuente {

    @Id
    @Column(length = 20)
    private String fuente;

    @Column(name = "periodo_inicio", nullable = false)
    private Instant periodoInicio;

    @Column(nullable = false)
    private int consumidas;

    @Column(name = "pausada_hasta")
    private Instant pausadaHasta;

    protected UsoFuente() {
        // Requerido por JPA
    }

    public void consumir() {
        this.consumidas++;
    }

    /** Empieza un periodo nuevo (el cupo del proveedor se reinició). */
    public void nuevoPeriodo(Instant inicio) {
        this.periodoInicio = inicio;
        this.consumidas = 0;
    }

    public void pausarHasta(Instant hasta) {
        this.pausadaHasta = hasta;
    }

    public String getFuente() {
        return fuente;
    }

    public Instant getPeriodoInicio() {
        return periodoInicio;
    }

    public int getConsumidas() {
        return consumidas;
    }

    public Instant getPausadaHasta() {
        return pausadaHasta;
    }
}
