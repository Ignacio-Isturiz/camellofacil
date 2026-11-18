package com.camellofacil.backend.cv.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Las últimas recomendaciones de la IA para la hoja de una persona. */
@Entity
@Table(name = "recomendacion_hoja_ia")
public class RecomendacionHojaIa {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "hoja_actualizada_en", nullable = false)
    private Instant hojaActualizadaEn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String items;

    @Column(nullable = false)
    private LocalDate dia;

    @Column(name = "intentos_hoy", nullable = false)
    private int intentosHoy;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected RecomendacionHojaIa() {}

    public RecomendacionHojaIa(UUID usuarioId, Instant hojaActualizadaEn, String items, LocalDate dia, int intentosHoy, Instant creadaEn) {
        this.usuarioId = usuarioId;
        this.hojaActualizadaEn = hojaActualizadaEn;
        this.items = items;
        this.dia = dia;
        this.intentosHoy = intentosHoy;
        this.creadaEn = creadaEn;
    }

    public UUID getUsuarioId() { return usuarioId; }
    public Instant getHojaActualizadaEn() { return hojaActualizadaEn; }
    public String getItems() { return items; }
    public LocalDate getDia() { return dia; }
    public int getIntentosHoy() { return intentosHoy; }

    public void reemplazar(Instant hojaActualizadaEn, String items, LocalDate dia, int intentosHoy, Instant creadaEn) {
        this.hojaActualizadaEn = hojaActualizadaEn;
        this.items = items;
        this.dia = dia;
        this.intentosHoy = intentosHoy;
        this.creadaEn = creadaEn;
    }
}
