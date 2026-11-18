package com.camellofacil.backend.reorganizaciones.app;

import com.camellofacil.backend.plantillas.app.ModelosPlantilla.DocumentoHoja;
import java.util.List;
import java.util.Map;

/** Tipos del módulo reorganizaciones que comparten las capas app y web. */
public final class ModelosReorganizacion {

    private ModelosReorganizacion() {}

    /**
     * Un cambio del plan. `clave` y `valor` dicen qué hace (p. ej. «perfil» con el texto nuevo); `aplicado` es lo
     * que la persona decide. Se guarda tal cual en la base.
     */
    public record CambioPlan(String id, String tipo, String titulo, String detalle, String seccionId, boolean aplicado, String clave, String valor) {}

    /** `tipo`: movido | redaccion | separado. Lo que ve la interfaz de cada cambio. */
    public record CambioPropuesto(String id, String tipo, String titulo, String detalle, boolean aplicado, String seccionId) {}

    public record PropuestaInfo(
            String id, String formato, String idioma, DocumentoHoja original, DocumentoHoja propuesta, List<CambioPropuesto> cambios,
            List<String> avisos) {}

    /** La redacción que propuso la IA y que ya pasó la verificación contra la hoja base. */
    public record RedaccionVerificada(String perfil, Map<String, String> descripciones) {
        public static RedaccionVerificada vacia() {
            return new RedaccionVerificada(null, Map.of());
        }
    }
}
