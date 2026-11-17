package com.camellofacil.backend.postulaciones.app;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Tipos de dominio del módulo de postulaciones. */
public final class ModelosPostulacion {

    private ModelosPostulacion() {}

    /** Con qué hoja de vida fue la persona a la página de la empresa. */
    public enum HojaUsada { BASE, ADAPTADA }

    public enum Orden { RECIENTES, ANTIGUAS, CARGO_AZ, CARGO_ZA }

    /** Una fila del historial. {@code ciudad} es null en las remotas sin ciudad. */
    public record PostulacionResumen(
            UUID id,
            UUID vacanteId,
            String cargo,
            String empresa,
            String ciudad,
            String modalidad,
            String tipo,
            Instant ultimoClicEn,
            int clics,
            HojaUsada hojaUsada) {}

    public record ListaPostulaciones(List<PostulacionResumen> items, long clicsUltimos90Dias) {}

    /** El clic quedó registrado; {@code url} es la página original de la oferta, donde termina la postulación. */
    public record ClicRegistrado(UUID postulacionId, String url) {}
}
