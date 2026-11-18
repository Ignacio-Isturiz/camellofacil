package com.camellofacil.backend.adaptaciones.app;

import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import java.util.List;

/** Tipos del módulo adaptaciones que comparten las capas app y web. */
public final class ModelosAdaptacion {

    private ModelosAdaptacion() {}

    /** `tipo`: redaccion | movido | destacado. */
    public record CambioAdaptacion(String id, String tipo, String titulo, String detalle) {}

    public record AdaptacionInfo(
            String id, String vacanteId, DocumentoHoja original, DocumentoHoja adaptada, List<CambioAdaptacion> cambios,
            List<String> avisos, int restantesHoy) {}

    public record EstadoAdaptacion(AdaptacionInfo adaptacion, int restantesHoy) {}

    public record CupoAdaptaciones(int usadas, int limite) {}
}
