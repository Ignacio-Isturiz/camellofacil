package com.camellofacil.backend.adaptaciones.app;

import com.camellofacil.backend.cv.app.ModelosHoja.ValoresHoja;
import java.util.List;

/**
 * Proveedor de IA que propone cómo ajustar una hoja de vida a una vacante. Hoy es Groq; cambiar de proveedor es
 * escribir otra implementación de esta interfaz.
 *
 * <p>Tanto la hoja como la vacante son contenido no confiable (pueden traer instrucciones dirigidas a la IA).
 * La respuesta nunca se usa tal cual: {@link VerificadorFidelidad} la compara con la hoja base y descarta todo
 * lo que no esté respaldado por ella.
 */
public interface AdaptadorIa {

    /**
     * @param hoja la hoja base SIN datos de contacto (nombre, teléfono, ciudad): la IA no los necesita
     */
    PropuestaIa adaptar(ValoresHoja hoja, VacanteIa vacante) throws AdaptadorIaException;

    /** Lo que se le cuenta a la IA de la vacante. */
    record VacanteIa(String cargo, String empresa, String descripcion, List<String> responsabilidades, List<String> requisitos) {}

    /** Lo que devuelve la IA, sin verificar. Cualquier campo puede venir null o con datos que no están en la hoja. */
    record PropuestaIa(String perfil, List<String> habilidades, List<ExperienciaIa> experiencia, List<String> avisos) {}

    /** El orden de la lista es el orden que la IA sugiere; solo la descripción se puede reescribir. */
    record ExperienciaIa(String id, String descripcion) {}

    /** El proveedor falló (red, límite de uso, respuesta inválida o sin configurar). */
    class AdaptadorIaException extends Exception {
        public AdaptadorIaException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
