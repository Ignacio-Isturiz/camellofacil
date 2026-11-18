package com.camellofacil.backend.cv.app;

import java.util.List;

/** Tipos del análisis Harvard / ATS que comparten las capas app y web del módulo cv. */
public final class ModelosAnalisis {

    private ModelosAnalisis() {}

    /** `nivel`: bajo | medio | alto. `valor` va de 0 a 100. */
    public record Puntaje(int valor, String nivel, String detalle) {}

    public record PuntajeAts(int valor, String nivel, String detalle, String opcionId) {}

    public record OpcionAts(String id, String nombre) {}

    /** `tipo`: falta | mejorar | bien. `origen`: reglas | ia. */
    public record Observacion(String id, String tipo, String titulo, String detalle, String origen) {}

    /** Una recomendación escrita por la IA (aún sin convertir en observación). */
    public record RecomendacionIa(String titulo, String detalle) {}

    /** `recomendacionesIa`: lista | pendiente | no_disponible. */
    public record Analisis(
            Puntaje harvard, PuntajeAts ats, List<OpcionAts> opcionesAts, List<Observacion> observaciones, String recomendacionesIa) {}
}
