package com.camellofacil.backend.cv.app;

import com.camellofacil.backend.cv.app.ModelosAnalisis.RecomendacionIa;
import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import java.util.List;

/**
 * Proveedor de IA que escribe recomendaciones para mejorar una hoja de vida. Hoy es Groq; cambiar de proveedor es
 * escribir otra implementación. El contenido de la hoja no es confiable (puede traer instrucciones para la IA):
 * las recomendaciones son solo texto que se le muestra a la persona, nunca se aplican solas a la hoja.
 */
public interface RecomendadorIa {

    /** @param hoja la hoja SIN datos de contacto: la IA no los necesita */
    List<RecomendacionIa> recomendar(ValoresHoja hoja) throws RecomendadorIaException;

    class RecomendadorIaException extends Exception {
        public RecomendadorIaException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
